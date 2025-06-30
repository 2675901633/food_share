from typing import List, Dict
from pydantic import BaseModel
import pandas as pd
import logging
from dataload import Dataload
from recall import Recall
from coarseranking import CoarseRanking
from reranking import Reranking


class RecommendationResponse(BaseModel):
    user_id: int
    recommended_gourmets: List[Dict[str, str]]
    recommended_categories: List[str]


class Recommend:
    @staticmethod
    def recommend_categories(user_id: int, interaction_df: pd.DataFrame, gourmet_df: pd.DataFrame,
                             category_df: pd.DataFrame, top_n: int=5) -> List[str]:
        try:
            user_interactions = interaction_df[interaction_df['user_id'] == user_id]
            logging.info(f"用户 {user_id} 有 {len(user_interactions)} 条交互")
            user_gourmets = user_interactions.merge(gourmet_df[['id', 'category_id']],
                                                    left_on='content_id', right_on='id')
            category_counts = user_gourmets.groupby('category_id').size().reset_index(name='count')
            category_counts = category_counts.merge(category_df, left_on='category_id', right_on='id', how='left')
            # 添加过滤NaN值的步骤
            categories = category_counts.sort_values(by='count', ascending=False)['name']
            categories = categories[categories.notna()].head(top_n).tolist()

            if not categories:
                logging.warning(f"用户 {user_id} 无特定类别，退回到流行类别")
                popularity = interaction_df.merge(gourmet_df[['id', 'category_id']],
                                                  left_on='content_id', right_on='id')
                popularity = popularity.groupby('category_id').size().reset_index(name='count')
                popularity = popularity.merge(category_df, left_on='category_id', right_on='id', how='left')
                # 这里也添加过滤NaN值的步骤
                categories = popularity.sort_values(by='count', ascending=False)['name']
                categories = categories[categories.notna()].head(top_n).tolist()
                if not categories:
                    logging.warning("无流行类别，返回默认类别")
                    # 默认类别也需要过滤NaN值
                    categories = category_df['name'].dropna().head(top_n).tolist()

            logging.info(f"用户 {user_id} 的类别推荐: {categories}")
            return categories
        except Exception as e:
            logging.error(f"类别推荐错误: {str(e)}")
            # 确保返回的默认类别也不包含NaN值
            return category_df['name'].dropna().head(top_n).tolist()

    @staticmethod
    def recommend(user_id: int, top_n: int = 5) -> RecommendationResponse:
        try:
            interaction_df = Dataload.load_interaction_data()
            gourmet_df = Dataload.load_gourmet_data()
            content_feature_df = Dataload.load_content_features()
            category_df = Dataload.load_category_data()
            user_df = Dataload.load_user_data()
            view_counts_df = Dataload.load_view_counts()

            gourmet_with_details = gourmet_df.merge(
                user_df[['id', 'user_name', 'user_avatar']],
                left_on='user_id',
                right_on='id',
                how='left',
                suffixes=('', '_user')
            )
            gourmet_with_details = gourmet_with_details.merge(
                view_counts_df,
                left_on='id',
                right_on='content_id',
                how='left'
            )
            gourmet_with_details['view_count'] = gourmet_with_details['view_count'].fillna(0).astype(int)
            # 在recommend方法中添加全局相似度矩阵计算
            item_similarity_df = Recall.calculate_global_item_similarity(interaction_df)
            cf_candidates = Recall.collaborative_filtering_recall(user_id, interaction_df, gourmet_df,
                                                                  item_similarity_df, top_n=400)
            logging.info(f"用户 {user_id} 的协同过滤候选: {len(cf_candidates)}")

            cb_candidates = Recall.content_based_recall(user_id, interaction_df, content_feature_df, gourmet_df,
                                                        top_n=400)
            logging.info(f"用户 {user_id} 的基于内容候选: {len(cb_candidates)}")

            pop_candidates = Recall.popularity_based_recall(gourmet_df, interaction_df, top_n=400)
            logging.info(f"用户 {user_id} 的流行度候选: {len(pop_candidates)}")
            weights = {
                'cf': 0.6,  # 协同过滤权重
                'cb': 0.2,  # 基于内容权重
                'pop': 0.2  # 流行度权重
            }
            # 构建加权候选集
            test_cf = list(set(cf_candidates))[:100]  # 限制每个来源的候选数量
            test_cb = list(set(cb_candidates))[:100]
            test_pop = list(set(pop_candidates))[:100]
            temp = []

            def calculate_candidates_scores(candidates, weight):
                result = []
                for idx, candidate in enumerate(candidates):
                    # 修改分数计算方式，减少排名影响
                    score = weight * (1 - 0.5 * idx / len(candidates))  # 降低排名衰减的影响
                    result.append((candidate, score))
                return result

            # 获取每个来源的候选项得分
            temp.extend(calculate_candidates_scores(test_cf, weights['cf']))
            temp.extend(calculate_candidates_scores(test_cb, weights['cb']))
            temp.extend(calculate_candidates_scores(test_pop, weights['pop']))

            # 对于相同的候选项，合并其得分
            candidate_scores = {}
            for candidate, score in temp:
                if candidate in candidate_scores:
                    candidate_scores[candidate] = max(candidate_scores[candidate], score)  # 使用最大值而不是累加
                else:
                    candidate_scores[candidate] = score

            # 按得分排序并获取候选项，增加候选数量
            candidates = sorted(candidate_scores.items(), key=lambda x: x[1], reverse=True)
            # 负反馈过滤
            user_negative = interaction_df[(interaction_df['user_id'] == user_id) & (interaction_df['score'] < 2)][
                'content_id'].unique()
            candidates = [c for c in candidates if c[0] not in user_negative]  # 修改这里，使用c[0]访问候选项ID
            candidates = [x[0] for x in candidates[:50]]  # 增加保留的候选数量到50个
            # 在召回阶段添加
            user_history = interaction_df[interaction_df['user_id'] == user_id]['content_id'].unique().tolist()
            user_history_sample = user_history[:min(10, len(user_history))]
            candidates.extend(user_history_sample)  # 添加部分用户历史记录
            logging.info(f"用户 {user_id} 的召回后总候选: {len(candidates)}")

            if not candidates and not gourmet_df.empty:
                logging.warning(f"用户 {user_id} 无候选，退回到所有美食ID")
                candidates = gourmet_df['id'].tolist()

            coarse_candidates = CoarseRanking.coarse_ranking(candidates, interaction_df, gourmet_df, top_n=200)
            logging.info(f"用户 {user_id} 的粗排后候选: {len(coarse_candidates)}")

            final_gourmets = Reranking.reranking(coarse_candidates, gourmet_df, category_df, interaction_df, user_id,
                                                 top_n)
            logging.info(f"用户 {user_id} 的重排后最终美食: {len(final_gourmets)}")

            categories = Recommend.recommend_categories(user_id, interaction_df, gourmet_df, category_df, top_n)

            recommended_gourmets = []
            for g in final_gourmets:
                gourmet = gourmet_with_details[gourmet_with_details['id'] == g]
                if not gourmet.empty:
                    recommended_gourmets.append({
                        "id": str(gourmet['id'].iloc[0]),
                        "title": gourmet['title'].iloc[0],
                        "categoryName": gourmet['category_name'].iloc[0] if pd.notna(
                            gourmet['category_name'].iloc[0]) else "Unknown",
                        "cover": gourmet['cover'].iloc[0] if pd.notna(gourmet['cover'].iloc[0]) else "",
                        "userAvatar": gourmet['user_avatar'].iloc[0] if pd.notna(
                            gourmet['user_avatar'].iloc[0]) else "",
                        "userName": gourmet['user_name'].iloc[0] if pd.notna(gourmet['user_name'].iloc[0]) else "",
                        "createTime": gourmet['create_time'].iloc[0].isoformat(),
                        "viewCount": str(gourmet['view_count'].iloc[0])
                    })
                else:
                    logging.warning(f"Gourmet ID {g} 未在 gourmet_with_details 中找到，跳过")

            response = RecommendationResponse(
                user_id=user_id,
                recommended_gourmets=recommended_gourmets,
                recommended_categories=categories
            )
            logging.info(f"用户 {user_id} 的推荐生成：{len(recommended_gourmets)} 个美食，{len(categories)} 个类别")
            return response
        except Exception as e:
            logging.error(f"推荐错误: {str(e)}")
            return RecommendationResponse(
                user_id=user_id,
                recommended_gourmets=[],
                recommended_categories=category_df['name'].head(top_n).tolist() if not category_df.empty else []
            )