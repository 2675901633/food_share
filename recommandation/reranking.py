from datetime import datetime
import numpy as np
import pandas as pd
import logging
from typing import List
import random


class Reranking:
    @staticmethod
    def reranking(candidates: List[int], gourmet_df: pd.DataFrame, category_df: pd.DataFrame,
                  interaction_df: pd.DataFrame, user_id: int, top_n: int = 15) -> List[int]:
        try:
            if not candidates:
                logging.warning("无候选提供给重排，返回空列表")
                return []

            candidates = [c for c in candidates if c in gourmet_df['id'].values]
            if not candidates:
                logging.warning("过滤后无有效候选，返回空列表")
                return []

            # 获取用户偏好类别
            user_gourmets = interaction_df[interaction_df['user_id'] == user_id].merge(
                gourmet_df[['id', 'category_id']], left_on='content_id', right_on='id'
            )
            preferred_categories = user_gourmets['category_id'].value_counts().index[:3].tolist()
            if not preferred_categories:
                preferred_categories = gourmet_df['category_id'].value_counts().index[:3].tolist()

            # 动态权重 - 优化个性化程度
            user_interaction_count = len(user_gourmets)
            # 根据用户交互模式动态调整权重
            if user_interaction_count > 20:  # 重度用户
                category_weight = 0.7  # 更注重类别匹配
                popularity_weight = 0.1  # 降低流行度影响
                freshness_weight = 0.2  # 保持新鲜度权重
            elif user_interaction_count > 10:  # 中度用户
                category_weight = 0.6
                popularity_weight = 0.2
                freshness_weight = 0.2
            elif user_interaction_count > 3:  # 轻度用户
                category_weight = 0.4
                popularity_weight = 0.3
                freshness_weight = 0.3
            else:  # 新用户
                category_weight = 0.3  # 降低类别匹配权重
                popularity_weight = 0.5  # 提高流行度权重
                freshness_weight = 0.2

            candidate_scores = []
            for gourmet_id in candidates:
                gourmet = gourmet_df[gourmet_df['id'] == gourmet_id]
                if gourmet.empty:
                    logging.warning(f"Gourmet ID {gourmet_id} 未在 gourmet_df 中找到，跳过")
                    continue
                days_since_creation = (datetime.now() - gourmet['create_time'].iloc[0]).days
                freshness_score = np.exp(-0.0036 * days_since_creation)
                category_id = gourmet['category_id'].iloc[0]
                category_preference = 2.0 if category_id in preferred_categories else 1.0
                category_popularity = len(gourmet_df[gourmet_df['category_id'] == category_id]) / len(gourmet_df)
                random_factor = random.uniform(0.95, 1.05)
                score = (freshness_weight * freshness_score +
                         category_weight * category_preference +
                         popularity_weight * category_popularity) * random_factor
                candidate_scores.append((gourmet_id, score))

            # 软约束多样性
            candidate_scores.sort(key=lambda x: x[1], reverse=True)
            selected = []
            selected_categories = set()
            for gourmet_id, score in candidate_scores:
                category_id = gourmet_df[gourmet_df['id'] == gourmet_id]['category_id'].iloc[0]
                if len(selected_categories) < 3 or category_id in selected_categories or len(selected) < top_n:
                    selected.append(gourmet_id)
                    selected_categories.add(category_id)
                if len(selected) >= top_n:
                    break

            # 若类别不足3，补充高分候选
            if len(selected_categories) < 3 and len(selected) < top_n:
                for gourmet_id, score in candidate_scores:
                    if gourmet_id not in selected:
                        selected.append(gourmet_id)
                        if len(selected) >= top_n:
                            break

            result = selected[:top_n]
            logging.info(f"重排完成: 从 {len(candidates)} 中选出 {len(result)} 个候选")
            return result
        except Exception as e:
            logging.error(f"重排错误: {str(e)}")
            return []