from datetime import datetime
from random import random

import numpy as np
import pandas as pd
from sklearn.decomposition import TruncatedSVD
from sklearn.metrics.pairwise import cosine_similarity
import logging
from typing import List


class Recall:
    @staticmethod
    def collaborative_filtering_recall(user_id: int, interaction_df: pd.DataFrame, gourmet_df: pd.DataFrame,
                                       item_similarity_df: pd.DataFrame, top_n: int) -> List[int]:
        try:
            # 获取用户交互的物品
            user_interacted = interaction_df[interaction_df['user_id'] == user_id]['content_id'].unique()

            if len(user_interacted) == 0:
                logging.warning(f"用户 {user_id} 无交互记录，使用混合召回策略")
                pop_candidates = Recall.popularity_based_recall(gourmet_df, interaction_df, top_n=int(top_n * 0.8))
                random_candidates = gourmet_df['id'].sample(min(int(top_n * 0.2), len(gourmet_df))).tolist()
                return list(set(pop_candidates + random_candidates))[:top_n]

            # 计算候选物品的得分
            candidate_scores = {}
            for item_id in user_interacted:
                if item_id in item_similarity_df.index:
                    similar_items = item_similarity_df[item_id].sort_values(ascending=False)
                    for similar_item, similarity in similar_items.items():
                        if similar_item not in user_interacted:
                            if similar_item in candidate_scores:
                                candidate_scores[similar_item] += similarity
                            else:
                                candidate_scores[similar_item] = similarity

            # 排序并返回top_n个候选
            candidates = sorted(candidate_scores.items(), key=lambda x: x[1], reverse=True)
            candidates = [c[0] for c in candidates if c[0] in gourmet_df['id'].values][:top_n]

            return candidates
        except Exception as e:
            logging.error(f"协同过滤召回错误: {str(e)}")
            return Recall.popularity_based_recall(gourmet_df, interaction_df, top_n)
    # @staticmethod
    # def collaborative_filtering_recall(user_id: int, interaction_df: pd.DataFrame, gourmet_df: pd.DataFrame,
    #                                    top_n: int) -> List[int]:
    #     try:
    #         # 输入验证
    #         required_columns = ['user_id', 'content_id', 'type', 'score']
    #         if not all(col in interaction_df.columns for col in required_columns):
    #             logging.error(f"interaction_df 缺少必要列: {required_columns}")
    #             return []
    #
    #         # 数据清洗 - 过滤异常值
    #         interaction_df = interaction_df.copy()
    #         # 过滤评分异常值
    #         q1 = interaction_df['score'].quantile(0.05)
    #         q3 = interaction_df['score'].quantile(0.95)
    #         interaction_df = interaction_df[(interaction_df['score'] >= q1) & (interaction_df['score'] <= q3)]
    #
    #         # 构建交互矩阵
    #         interaction_matrix = interaction_df.pivot_table(
    #             index='user_id',
    #             columns='content_id',
    #             values='score',
    #             aggfunc='mean',
    #             fill_value=0
    #         )
    #
    #         # 改进权重分配 - 根据交互类型和时间衰减
    #         weights = {1: 3, 2: 4, 3: 1, 4: 'score'}
    #         weight_df = interaction_df[['user_id', 'content_id', 'type', 'score', 'create_time']].copy()
    #         # 添加时间衰减因子
    #         weight_df['time_decay'] = weight_df['create_time'].apply(
    #             lambda x: np.exp(-0.0018 * (datetime.now() - x).days)  # 减小衰减系数
    #         )
    #         weight_df['weight'] = weight_df.apply(
    #             lambda row: (row['score'] if row['type'] == 4 else weights.get(row['type'], 0)) * row['time_decay'],
    #             axis=1
    #         )
    #
    #         for _, row in weight_df.iterrows():
    #             if row['user_id'] in interaction_matrix.index and row['content_id'] in interaction_matrix.columns:
    #                 interaction_matrix.loc[row['user_id'], row['content_id']] = row['weight']
    #         interaction_matrix = interaction_matrix.fillna(0)
    #
    #         # 冷启动用户处理
    #         if user_id not in interaction_matrix.index or interaction_matrix.loc[user_id].sum() < 2:
    #             logging.warning(f"用户 {user_id} 交互记录不足，使用混合召回策略")
    #             # 返回流行度和随机混合召回
    #             pop_candidates = Recall.popularity_based_recall(gourmet_df, interaction_df, top_n=int(top_n * 0.8))
    #             # 添加一些随机推荐以增加探索性
    #             random_candidates = gourmet_df['id'].sample(min(int(top_n * 0.2), len(gourmet_df))).tolist()
    #             return list(set(pop_candidates + random_candidates))[:top_n]
    #
    #         # 动态调整SVD组件数量
    #         user_count = interaction_matrix.shape[0]
    #         item_count = interaction_matrix.shape[1]
    #         sparsity = 1.0 - (interaction_matrix.astype(bool).sum().sum() / (user_count * item_count))
    #
    #         # 根据稀疏度调整组件数量
    #         if sparsity > 0.99:
    #             n_components = min(20, interaction_matrix.shape[1] // 4)
    #         elif sparsity > 0.95:
    #             n_components = min(30, interaction_matrix.shape[1] // 3)
    #         else:
    #             n_components = min(50, interaction_matrix.shape[1] // 2)
    #
    #         svd = TruncatedSVD(n_components=n_components, random_state=42)
    #         user_factors = svd.fit_transform(interaction_matrix)
    #         item_factors = svd.components_.T
    #
    #         # 计算用户对所有物品的预测评分
    #         user_idx = interaction_matrix.index.get_loc(user_id)
    #         predicted_scores = np.dot(user_factors[user_idx], item_factors.T)
    #         predicted_scores_df = pd.DataFrame({
    #             'content_id': interaction_matrix.columns,
    #             'score': predicted_scores
    #         })
    #
    #         # 召回候选
    #         user_interacted = interaction_df[interaction_df['user_id'] == user_id]['content_id'].unique()
    #         candidates = predicted_scores_df[~predicted_scores_df['content_id'].isin(user_interacted)]
    #         candidates = candidates[candidates['content_id'].isin(gourmet_df['id'])]
    #
    #         # 增加类别多样性
    #         if len(candidates) > top_n * 2:
    #             # 获取用户偏好类别
    #             user_gourmets = interaction_df[interaction_df['user_id'] == user_id].merge(
    #                 gourmet_df[['id', 'category_id']], left_on='content_id', right_on='id'
    #             )
    #             preferred_categories = user_gourmets['category_id'].value_counts().index.tolist()
    #
    #             # 确保每个类别都有代表
    #             diverse_candidates = []
    #             category_counts = {}
    #
    #             # 先选择用户偏好类别的高分项目
    #             candidates_with_category = candidates.merge(
    #                 gourmet_df[['id', 'category_id']], left_on='content_id', right_on='id'
    #             )
    #
    #             # 按分数排序
    #             sorted_candidates = candidates_with_category.sort_values(by='score', ascending=False)
    #
    #             # 先选择偏好类别的项目
    #             for _, row in sorted_candidates.iterrows():
    #                 category = row['category_id']
    #                 if category in preferred_categories and (
    #                         category not in category_counts or category_counts[category] < top_n // 3):
    #                     diverse_candidates.append(row['content_id'])
    #                     category_counts[category] = category_counts.get(category, 0) + 1
    #                 if len(diverse_candidates) >= top_n * 0.6:
    #                     break
    #
    #             # 再选择其他高分项目
    #             for _, row in sorted_candidates.iterrows():
    #                 if row['content_id'] not in diverse_candidates:
    #                     category = row['category_id']
    #                     if category not in category_counts or category_counts[category] < top_n // 4:
    #                         diverse_candidates.append(row['content_id'])
    #                         category_counts[category] = category_counts.get(category, 0) + 1
    #                 if len(diverse_candidates) >= top_n:
    #                     break
    #
    #             # 如果还不够，添加剩余的高分项目
    #             if len(diverse_candidates) < top_n:
    #                 remaining = candidates.sort_values(by='score', ascending=False)['content_id'].tolist()
    #                 for item in remaining:
    #                     if item not in diverse_candidates:
    #                         diverse_candidates.append(item)
    #                     if len(diverse_candidates) >= top_n:
    #                         break
    #
    #             candidates = diverse_candidates[:top_n]
    #         else:
    #             candidates = candidates.sort_values(by='score', ascending=False)['content_id'].head(top_n).tolist()
    #
    #         logging.info(f"协同过滤召回用户 {user_id}：{len(candidates)} 个候选")
    #         return candidates
    #     except ValueError as ve:
    #         logging.error(f"协同过滤召回值错误: {str(ve)}")
    #         return Recall.popularity_based_recall(gourmet_df, interaction_df, top_n=top_n)  # 错误时使用流行度召回
    #     except KeyError as ke:
    #         logging.error(f"协同过滤召回键错误: {str(ke)}")
    #         return Recall.popularity_based_recall(gourmet_df, interaction_df, top_n=top_n)  # 错误时使用流行度召回
    #     except Exception as e:
    #         logging.error(f"协同过滤召回未知错误: {str(e)}")
    #         return Recall.popularity_based_recall(gourmet_df, interaction_df, top_n=top_n)  # 错误时使用流行度召回

    @staticmethod
    def content_based_recall(user_id: int, interaction_df: pd.DataFrame, content_feature_df: pd.DataFrame,
                             gourmet_df: pd.DataFrame, top_n: int ) -> List[int]:
        try:
            # 输入验证
            required_columns = ['gourmet_id', 'feature_vector']
            if content_feature_df.empty or not all(col in content_feature_df.columns for col in required_columns):
                logging.warning(f"用户 {user_id} 的 content_feature_df 为空或缺少必要列: {required_columns}")
                return Recall.popularity_based_recall(gourmet_df, interaction_df, top_n)  # 错误时使用流行度召回

            user_interacted = interaction_df[interaction_df['user_id'] == user_id]['content_id'].unique()
            logging.info(f"用户 {user_id} 有 {len(user_interacted)} 个交互美食")
            if len(user_interacted) == 0:
                logging.warning(f"用户 {user_id} 无交互记录，返回流行度召回")
                return Recall.popularity_based_recall(gourmet_df, interaction_df, top_n)

            # 获取用户偏好类别
            user_gourmets = interaction_df[interaction_df['user_id'] == user_id].merge(
                gourmet_df[['id', 'category_id']], left_on='content_id', right_on='id'
            )
            preferred_categories = user_gourmets['category_id'].value_counts().index[:3].tolist()
            if not preferred_categories:  # 添加容错处理
                preferred_categories = gourmet_df['category_id'].value_counts().index[:3].tolist()

            # 计算类别集中度（熵）
            if not user_gourmets.empty:
                category_dist = user_gourmets['category_id'].value_counts(normalize=True)
                entropy = -np.sum(category_dist * np.log2(category_dist + 1e-10))
                max_entropy = np.log2(len(category_dist)) if len(category_dist) > 0 else 1
                concentration = 1 - entropy / max_entropy
                category_boost_factor = 1.5 + 0.5 * concentration
            else:
                category_boost_factor = 1.5

            # 改进特征向量处理
            user_features = content_feature_df[content_feature_df['gourmet_id'].isin(user_interacted)]['feature_vector']
            if user_features.empty or user_features.isna().all():
                logging.warning(f"用户 {user_id} 的交互美食无有效特征向量，使用流行度召回")
                return Recall.popularity_based_recall(gourmet_df, interaction_df, top_n)

            # 特征向量验证和处理
            valid_features = []
            for idx, f in user_features.items():
                if isinstance(f, list) and len(f) > 0:
                    # 更宽容的特征向量验证
                    if all(isinstance(x, (int, float)) for x in f):
                        # 处理全0向量
                        if not any(f):
                            logging.warning(
                                f"gourmet_id {content_feature_df.loc[idx, 'gourmet_id']} 的特征向量全为 0，使用小随机值替代")
                            # 使用小随机值替代全0向量
                            valid_features.append([np.random.uniform(0.001, 0.01) for _ in range(len(f))])
                        else:
                            valid_features.append(f)

            logging.info(f"用户 {user_id} 有 {len(valid_features)} 个有效特征向量")
            if not valid_features:
                logging.warning(f"用户 {user_id} 无有效特征向量，使用流行度召回")
                return Recall.popularity_based_recall(gourmet_df, interaction_df, top_n)

            # 标准化向量维度
            max_dim = max(len(f) for f in valid_features)
            normalized_features = []
            for f in valid_features:
                if len(f) < max_dim:
                    # 填充较短的向量
                    normalized_f = f + [0] * (max_dim - len(f))
                else:
                    normalized_f = f
                # 归一化向量
                norm = np.linalg.norm(normalized_f)
                if norm > 0:
                    normalized_f = [x / norm for x in normalized_f]
                normalized_features.append(normalized_f)

            user_vector = np.mean([np.array(f) for f in normalized_features], axis=0)

            # 处理内容特征向量
            all_features = []
            valid_indices = []
            for i, f in enumerate(content_feature_df['feature_vector']):
                if isinstance(f, list) and len(f) > 0:
                    # 标准化维度
                    if len(f) < len(user_vector):
                        normalized_f = f + [0] * (len(user_vector) - len(f))
                    elif len(f) > len(user_vector):
                        normalized_f = f[:len(user_vector)]
                    else:
                        normalized_f = f

                    # 处理全0向量
                    if not any(normalized_f):
                        normalized_f = [np.random.uniform(0.001, 0.01) for _ in range(len(user_vector))]

                    # 归一化向量
                    norm = np.linalg.norm(normalized_f)
                    if norm > 0:
                        normalized_f = [x / norm for x in normalized_f]

                    all_features.append(normalized_f)
                    valid_indices.append(i)

            if not all_features:
                logging.warning(f"用户 {user_id} 的 content_feature_df 无有效特征向量，使用流行度召回")
                return Recall.popularity_based_recall(gourmet_df, interaction_df, top_n)

            all_features = np.array(all_features)
            content_feature_df = content_feature_df.iloc[valid_indices].copy()

            # 计算相似度
            similarities = cosine_similarity([user_vector], all_features)[0]
            content_feature_df['similarity'] = similarities

            # 优先选择用户偏好类别的美食
            candidates = content_feature_df.merge(gourmet_df[['id', 'category_id']], left_on='gourmet_id',
                                                  right_on='id')
            candidates = candidates[~candidates['gourmet_id'].isin(user_interacted)]

            # 动态类别加权
            candidates['category_boost'] = candidates['category_id'].apply(
                lambda x: category_boost_factor if x in preferred_categories else 1.0)

            # 添加流行度因子
            popularity = interaction_df.groupby('content_id').size().reset_index(name='interaction_count')
            candidates = candidates.merge(popularity, left_on='gourmet_id', right_on='content_id', how='left')
            candidates['interaction_count'] = candidates['interaction_count'].fillna(0)

            # 归一化流行度
            max_count = candidates['interaction_count'].max() if candidates['interaction_count'].max() > 0 else 1
            candidates['popularity_factor'] = 0.3 + 0.7 * (candidates['interaction_count'] / max_count)

            # 综合评分
            candidates['score'] = candidates['similarity'] * candidates['category_boost'] * candidates[
                'popularity_factor']
            candidates = candidates.sort_values(by='score', ascending=False)['gourmet_id'].head(top_n).tolist()

            logging.info(f"基于内容的召回用户 {user_id}：{len(candidates)} 个候选")
            return candidates
        except ValueError as ve:
            logging.error(f"基于内容召回值错误: {str(ve)}")
            return Recall.popularity_based_recall(gourmet_df, interaction_df, top_n)  # 错误时使用流行度召回
        except KeyError as ke:
            logging.error(f"基于内容召回键错误: {str(ke)}")
            return Recall.popularity_based_recall(gourmet_df, interaction_df, top_n)  # 错误时使用流行度召回
        except Exception as e:
            logging.error(f"基于内容的召回错误: {str(e)}")
            return Recall.popularity_based_recall(gourmet_df, interaction_df, top_n)  # 错误时使用流行度召回

    @staticmethod
    def popularity_based_recall(gourmet_df: pd.DataFrame, interaction_df: pd.DataFrame, top_n: int ) -> List[int]:
        try:
            if gourmet_df.empty or 'id' not in gourmet_df.columns:
                logging.warning("gourmet_df 为空或缺少 id 列，返回空列表")
                return []

            candidates = []
            if not interaction_df.empty:
                interaction_df = interaction_df.copy()

                # 改进时间衰减计算
                current_time = datetime.now()
                max_days = (current_time - interaction_df['create_time'].min()).days
                decay_factor = 2.0 / max_days if max_days > 0 else 0.01  # 动态衰减因子

                interaction_df['time_decay'] = interaction_df['create_time'].apply(
                    lambda x: np.exp(-decay_factor * (current_time - x).days)
                )

                # 按交互类型分组计算流行度
                popularity = interaction_df.groupby('content_id').agg({
                    'type': 'count',
                    'time_decay': 'mean',
                    'score': 'mean',
                    'create_time': 'max'  # 最近交互时间
                }).reset_index()

                # 计算最近度分数
                popularity['recency'] = popularity['create_time'].apply(
                    lambda x: np.exp(-0.01 * (current_time - x).days)
                )

                # 改进流行度计算权重
                popularity['score'] = (
                                              popularity['type'] * 0.3 +  # 交互次数
                                              popularity['score'] * 0.4 +  # 评分
                                              popularity['recency'] * 0.3  # 最近度
                                      ) * popularity['time_decay']

                popularity = popularity[popularity['content_id'].isin(gourmet_df['id'])]

                # 改进类别多样性约束
                candidates = []
                category_counts = {}
                category_limit = max(top_n // 5, 3)  # 每个类别的上限

                # 合并类别信息
                popularity_with_category = popularity.merge(
                    gourmet_df[['id', 'category_id']],
                    left_on='content_id',
                    right_on='id'
                )

                # 按分数排序
                for _, row in popularity_with_category.sort_values(by='score', ascending=False).iterrows():
                    category = row['category_id']
                    if category not in category_counts:
                        category_counts[category] = 0

                    if category_counts[category] < category_limit:
                        candidates.append(row['content_id'])
                        category_counts[category] += 1

                    if len(candidates) >= top_n:
                        break

                # 如果候选不足，添加剩余高分项目
                if len(candidates) < top_n:
                    remaining = popularity_with_category.sort_values(by='score', ascending=False)['content_id'].tolist()
                    for item in remaining:
                        if item not in candidates:
                            candidates.append(item)
                        if len(candidates) >= top_n:
                            break

                logging.info(f"基于流行度的召回：{len(candidates)} 个候选")

            # 如果仍然不足，随机选择
            if len(candidates) < top_n and not gourmet_df.empty:
                logging.warning(f"流行度候选不足({len(candidates)}个)，添加随机选择补充至{top_n}个")
                remaining_ids = set(gourmet_df['id']) - set(candidates)
                if remaining_ids:
                    random_ids = random.sample(list(remaining_ids), min(top_n - len(candidates), len(remaining_ids)))
                    candidates.extend(random_ids)

            logging.info(f"最终流行度召回：{len(candidates)} 个候选: {candidates[:10]}...")
            return candidates
        except ValueError as ve:
            logging.error(f"流行度召回值错误: {str(ve)}")
            # 出错时返回随机选择
            return gourmet_df['id'].sample(min(top_n, len(gourmet_df))).tolist() if not gourmet_df.empty else []
        except KeyError as ke:
            logging.error(f"流行度召回键错误: {str(ke)}")
            return gourmet_df['id'].sample(min(top_n, len(gourmet_df))).tolist() if not gourmet_df.empty else []
        except Exception as e:
            logging.error(f"流行度召回错误: {str(e)}")
            return gourmet_df['id'].sample(min(top_n, len(gourmet_df))).tolist() if not gourmet_df.empty else []

    @staticmethod
    def calculate_global_item_similarity(interaction_df: pd.DataFrame) -> pd.DataFrame:
        # 构建交互矩阵
        interaction_matrix = interaction_df.pivot_table(
            index='user_id',
            columns='content_id',
            values='score',
            aggfunc='mean',
            fill_value=0
        )

        # SVD降维
        user_count = interaction_matrix.shape[0]
        item_count = interaction_matrix.shape[1]
        sparsity = 1.0 - (interaction_matrix.astype(bool).sum().sum() / (user_count * item_count))

        # 根据稀疏度调整组件数量
        if sparsity > 0.99:
            n_components = min(20, interaction_matrix.shape[1] // 4)
        elif sparsity > 0.95:
            n_components = min(30, interaction_matrix.shape[1] // 3)
        else:
            n_components = min(50, interaction_matrix.shape[1] // 2)

        svd = TruncatedSVD(n_components=n_components, random_state=42)
        item_factors = svd.fit_transform(interaction_matrix.T)

        # 计算物品相似度矩阵
        item_similarity = cosine_similarity(item_factors)

        # 转换为DataFrame便于查询
        item_similarity_df = pd.DataFrame(
            item_similarity,
            index=interaction_matrix.columns,
            columns=interaction_matrix.columns
        )

        return item_similarity_df