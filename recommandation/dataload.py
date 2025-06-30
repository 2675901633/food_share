import pandas as pd
from sqlalchemy import create_engine
import json
import logging
from config import Config
import numpy as np
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    filename='dataload_test.log',
    encoding='utf-8'
)

# 加载 top_keywords
try:
    with open('top_keywords.json', 'r', encoding='utf-8') as f:
        top_keywords = json.load(f)
    logging.info(f"加载了 {len(top_keywords)} 个 top_keywords")
except FileNotFoundError:
    logging.error("top_keywords.json 文件不存在")
    top_keywords = []
except Exception as e:
    logging.error(f"加载 top_keywords.json 失败: {str(e)}")
    top_keywords = []

# 初始化 SQLAlchemy 引擎
engine = create_engine(Config.SQLALCHEMY_DATABASE_URI, echo=False)

class Dataload:
    @staticmethod
    def load_interaction_data():
        """从 interaction 表加载用户交互数据"""
        try:
            query = """
            SELECT i.user_id, i.content_id, i.type, i.score, i.create_time, i.content_type
            FROM interaction i
            INNER JOIN gourmet g ON i.content_id = g.id
            WHERE i.content_type IN ('UPVOTE', 'COLLECTION', 'VIEW', 'RATING')
            AND g.is_audit = 1 AND g.is_publish = 1
            """
            df = pd.read_sql(query, engine)
            df['score'] = df['score'].fillna(0)
            df = df.dropna(subset=['user_id', 'content_id', 'type'])
            logging.info(f"加载了 {len(df)} 条交互记录")
            return df
        except Exception as e:
            logging.error(f"加载交互数据失败: {str(e)}")
            raise

    @staticmethod
    def load_gourmet_data():
        """从 gourmet 表加载美食数据"""
        try:
            query = """
            SELECT g.id, g.title, g.category_id, c.name as category_name, g.create_time, g.cover as cover, g.user_id
            FROM gourmet g
            LEFT JOIN category c ON g.category_id = c.id
            WHERE g.is_audit = 1 AND g.is_publish = 1
            """
            df = pd.read_sql(query, engine)
            logging.info(f"加载了 {len(df)} 条美食记录")
            return df
        except Exception as e:
            logging.error(f"加载美食数据失败: {str(e)}")
            raise

    @staticmethod
    def load_content_features():
        """从 feature_vectors.csv 加载固定维度特征向量"""
        try:
            if not top_keywords:
                logging.error("top_keywords 为空，无法加载特征向量")
                return pd.DataFrame(columns=['gourmet_id', 'feature_vector'])

            # 读取预处理的特征向量
            df = pd.read_csv('feature_vectors.csv')
            if df.empty or 'gourmet_id' not in df.columns or 'feature_vector_fixed' not in df.columns:
                logging.warning("feature_vectors.csv 无效或为空，返回空 DataFrame")
                return pd.DataFrame(columns=['gourmet_id', 'feature_vector'])

            def parse_fixed_vector(fv):
                try:
                    if pd.isna(fv):
                        logging.warning(f"gourmet_id {df.loc[df['feature_vector_fixed'] == fv, 'gourmet_id'].iloc[0]} 的特征向量为 NaN")
                        return None
                    vector = json.loads(fv)
                    if not isinstance(vector, list) or len(vector) != len(top_keywords) or not all(isinstance(x, (int, float)) for x in vector):
                        logging.warning(f"gourmet_id {df.loc[df['feature_vector_fixed'] == fv, 'gourmet_id'].iloc[0]} 的特征向量无效: {vector}")
                        return None
                    if not any(vector):
                        logging.warning(f"gourmet_id {df.loc[df['feature_vector_fixed'] == fv, 'gourmet_id'].iloc[0]} 的特征向量全为 0")
                        return None
                    return vector
                except json.JSONDecodeError as e:
                    logging.error(f"解析 gourmet_id {df.loc[df['feature_vector_fixed'] == fv, 'gourmet_id'].iloc[0]} 的特征向量失败: {str(e)}")
                    return None
                except Exception as e:
                    logging.error(f"解析 gourmet_id {df.loc[df['feature_vector_fixed'] == fv, 'gourmet_id'].iloc[0]} 的特征向量失败: {str(e)}")
                    return None

            df['feature_vector'] = df['feature_vector_fixed'].apply(parse_fixed_vector)
            df = df[df['feature_vector'].notna()][['gourmet_id', 'feature_vector']]
            if df.empty:
                logging.warning("解析后无有效特征向量，返回空 DataFrame")
            logging.info(f"加载了 {len(df)} 条有效特征向量记录")
            return df
        except FileNotFoundError:
            logging.error("feature_vectors.csv 文件不存在")
            return pd.DataFrame(columns=['gourmet_id', 'feature_vector'])
        except Exception as e:
            logging.error(f"加载特征数据失败: {str(e)}")
            return pd.DataFrame(columns=['gourmet_id', 'feature_vector'])

    @staticmethod
    def load_category_data():
        """从 category 表加载类别数据"""
        try:
            query = "SELECT id, name FROM category"
            df = pd.read_sql(query, engine)
            logging.info(f"加载了 {len(df)} 条类别记录")
            return df
        except Exception as e:
            logging.error(f"加载类别数据失败: {str(e)}")
            raise

    @staticmethod
    def load_user_data():
        """从 user 表加载用户数据"""
        try:
            query = """
            SELECT id, user_name, user_avatar
            FROM user
            """
            df = pd.read_sql(query, engine)
            logging.info(f"加载了 {len(df)} 条用户记录")
            return df
        except Exception as e:
            logging.error(f"加载用户数据失败: {str(e)}")
            raise

    @staticmethod
    def load_view_counts():
        """从 interaction 表加载美食浏览量"""
        try:
            query = """
            SELECT content_id, COUNT(*) as view_count
            FROM interaction
            WHERE content_type = 'VIEW'
            GROUP BY content_id
            """
            df = pd.read_sql(query, engine)
            logging.info(f"加载了 {len(df)} 条浏览量记录")
            return df
        except Exception as e:
            logging.error(f"加载浏览量失败: {str(e)}")
            raise