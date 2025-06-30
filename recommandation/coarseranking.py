from datetime import datetime
import numpy as np
import pandas as pd
import logging
from typing import List


class CoarseRanking:
    @staticmethod
    def coarse_ranking(candidates: List[int], interaction_df: pd.DataFrame, gourmet_df: pd.DataFrame,
                       top_n: int, user_id: int = None) -> List[int]:
        try:
            if not candidates:
                logging.warning("无候选提供给粗排，返回空列表")
                return []

            logging.info(f"粗排输入：{len(candidates)} 个候选: {candidates[:10]}...")

            candidates = [c for c in candidates if c in gourmet_df['id'].values]
            if not candidates:
                logging.warning("过滤后无有效候选，返回空列表")
                return []

            # 获取用户历史交互信息
            user_interactions = interaction_df[interaction_df['user_id'] == user_id] if user_id else pd.DataFrame()
            user_interaction_count = len(user_interactions)

            # 获取用户偏好类别
            preferred_categories = []
            if not user_interactions.empty:
                user_gourmets = user_interactions.merge(
                    gourmet_df[['id', 'category_id']], left_on='content_id', right_on='id'
                )
                preferred_categories = user_gourmets['category_id'].value_counts().index.tolist()

            # 动态权重调整
            interaction_weight = 0.5 + min(0.3, user_interaction_count * 0.01)  # 交互越多权重越高
            view_weight = 0.5 - min(0.3, user_interaction_count * 0.01)  # 交互越多浏览权重越低
            category_weight = 0.2  # 类别匹配权重

            # 获取浏览数据
            view_counts = interaction_df[interaction_df['content_type'] == 'VIEW'].groupby(
                'content_id').size().to_dict()

            # 计算浏览分位数，用于长尾内容提升
            view_values = list(view_counts.values())
            if view_values:
                view_q1 = np.percentile(view_values, 25)
                view_q3 = np.percentile(view_values, 75)
            else:
                view_q1 = view_q3 = 0

            # 计算每个候选的分数
            candidate_scores = []
            for gourmet_id in candidates:
                # 基础交互分数
                interactions = interaction_df[interaction_df['content_id'] == gourmet_id]
                interaction_score = 0
                if not interactions.empty:
                    # 根据交互类型加权
                    weights = {1: 3, 2: 4, 3: 1, 4: 2}  # 调整权重
                    for _, row in interactions.iterrows():
                        if row['type'] == 4:
                            interaction_score += row['score'] * 2  # 加大评分权重
                        else:
                            interaction_score += weights.get(row['type'], 0)

                    # 归一化交互分数
                    interaction_score = min(10, interaction_score / len(interactions))

                # 浏览量分数
                view_count = view_counts.get(gourmet_id, 0)

                # 长尾内容提升
                if view_count < view_q1:
                    view_boost = 1.3  # 显著提升低浏览量内容
                elif view_count < view_q3:
                    view_boost = 1.1  # 轻微提升中等浏览量内容
                else:
                    view_boost = 1.0  # 高浏览量内容不提升

                # 获取美食信息
                gourmet = gourmet_df[gourmet_df['id'] == gourmet_id]
                if gourmet.empty:
                    continue

                # 类别匹配分数
                category_score = 1.5 if gourmet['category_id'].iloc[0] in preferred_categories else 1.0

                # 时间衰减 - 使用更平缓的衰减曲线
                days_old = (datetime.now() - gourmet['create_time'].iloc[0]).days
                time_decay = 1.0 / (1.0 + 0.01 * days_old)  # 使用逻辑衰减而非指数衰减

                # 计算最终分数
                final_score = (
                                      interaction_score * interaction_weight +
                                      view_count * view_weight * view_boost +
                                      category_score * category_weight
                              ) * time_decay

                candidate_scores.append((gourmet_id, final_score))

            # 排序并返回结果
            candidate_scores.sort(key=lambda x: x[1], reverse=True)
            ranked_candidates = [x[0] for x in candidate_scores][:top_n]

            logging.info(
                f"粗排完成：从 {len(candidates)} 中选出 {len(ranked_candidates)} 个候选: {ranked_candidates[:10]}...")
            return ranked_candidates
        except Exception as e:
            logging.error(f"粗排错误: {str(e)}")
            return candidates[:min(len(candidates), top_n)]  # 出错时返回原始候选