import pandas as pd
from sqlalchemy import create_engine
import json
import logging
from config import Config
from collections import Counter
import numpy as np

# 初始化 SQLAlchemy 引擎
engine = create_engine(Config.SQLALCHEMY_DATABASE_URI, echo=False)
top_keywords = []

def extract_feature_vectors():
    """从 content_feature 表中读取 feature_vector 数据并生成固定维度向量"""
    global top_keywords
    try:
        # SQL 查询
        query = """
        SELECT gourmet_id, feature_vector
        FROM content_feature
        """
        # 读取数据
        df = pd.read_sql(query, engine)
        logging.info(f"成功读取 {len(df)} 条 feature_vector 记录")

        # 收集所有关键词并记录键集合
        keyword_counts = Counter()
        parsed_vectors = []
        invalid_records = []
        for _, row in df.iterrows():
            gourmet_id = row['gourmet_id']
            try:
                if pd.isna(row['feature_vector']) or not row['feature_vector']:
                    logging.warning(f"gourmet_id {gourmet_id} 的 feature_vector 为 NaN 或空")
                    parsed_vectors.append(None)
                    invalid_records.append(gourmet_id)
                    continue
                parsed = json.loads(row['feature_vector'])
                if not isinstance(parsed, dict) or not parsed:
                    logging.warning(f"gourmet_id {gourmet_id} 的 feature_vector 无效: {parsed}")
                    parsed_vectors.append(None)
                    invalid_records.append(gourmet_id)
                    continue
                # 验证键值对的有效性
                if not all(isinstance(k, str) and isinstance(v, (int, float)) for k, v in parsed.items()):
                    logging.warning(f"gourmet_id {gourmet_id} 的 feature_vector 包含无效键值对: {parsed}")
                    parsed_vectors.append(None)
                    invalid_records.append(gourmet_id)
                    continue
                keyword_counts.update(parsed.keys())
                parsed_vectors.append(parsed)
                logging.debug(f"gourmet_id {gourmet_id} 的 feature_vector 键: {list(parsed.keys())}")
            except json.JSONDecodeError as e:
                logging.error(f"解析 gourmet_id {gourmet_id} 的 feature_vector 失败: {str(e)}")
                parsed_vectors.append(None)
                invalid_records.append(gourmet_id)
            except Exception as e:
                logging.error(f"处理 gourmet_id {gourmet_id} 失败: {str(e)}")
                parsed_vectors.append(None)
                invalid_records.append(gourmet_id)

        df['feature_vector_parsed'] = parsed_vectors
        df['is_valid'] = df['feature_vector_parsed'].apply(lambda x: x is not None and len(x) > 0)

        # 统计有效和无效记录
        valid_count = df['is_valid'].sum()
        logging.info(f"有效 feature_vector 记录: {valid_count}")
        logging.info(f"无效 feature_vector 记录: {len(df) - valid_count}")
        if invalid_records:
            logging.warning(f"无效记录的 gourmet_id: {invalid_records[:10]}{'...' if len(invalid_records) > 10 else ''}")

        # 生成 top_keywords（所有键）
        top_keywords = sorted(keyword_counts.keys())
        if not top_keywords:
            logging.error("未提取到任何关键词，top_keywords 为空")
            raise ValueError("top_keywords 为空")
        logging.info(f"提取到 {len(top_keywords)} 个唯一关键词")
        logging.debug(f"前10个关键词: {top_keywords[:10]}")

        # 保存 top_keywords 到文件
        with open('top_keywords.json', 'w', encoding='utf-8') as f:
            json.dump(top_keywords, f, ensure_ascii=False)
        logging.info("top_keywords 已保存到 top_keywords.json")

        # 转换为固定维度向量
        def to_fixed_vector(parsed, gourmet_id):
            if parsed is None or not isinstance(parsed, dict):
                return None
            # 检查键交集
            parsed_keys = set(parsed.keys())
            top_keys = set(top_keywords)
            common_keys = parsed_keys & top_keys
            if not common_keys:
                logging.warning(f"gourmet_id {gourmet_id} 的 feature_vector 键与 top_keywords 无交集: {parsed_keys}")
                return None
            vector = [parsed.get(key, 0.0) for key in top_keywords]
            if not any(vector):
                logging.warning(f"gourmet_id {gourmet_id} 的 feature_vector 转换后全为 0: {parsed}")
                return None
            logging.debug(f"gourmet_id {gourmet_id} 的向量非 0 元素数: {sum(1 for x in vector if x != 0)}")
            return json.dumps(vector, ensure_ascii=False)  # 保存为 JSON 字符串，保留原始浮点数

        df['feature_vector_fixed'] = [to_fixed_vector(parsed, gourmet_id) for parsed, gourmet_id in zip(df['feature_vector_parsed'], df['gourmet_id'])]
        df['is_valid'] = df['feature_vector_fixed'].apply(lambda x: x is not None)

        # 保存结果到 CSV
        output_file = 'feature_vectors.csv'
        df[['gourmet_id', 'feature_vector_fixed', 'is_valid']].to_csv(output_file, index=False, encoding='utf-8')
        logging.info(f"固定维度向量数据已保存到 {output_file}")

        # 保存解析后的 JSON
        df_valid = df[df['is_valid']].copy()
        df_valid[['gourmet_id', 'feature_vector_parsed']].to_json('feature_vectors_parsed.json', orient='records', lines=True, force_ascii=False)
        logging.info(f"解析后的 JSON 数据已保存到 feature_vectors_parsed.json")

        return df

    except Exception as e:
        logging.error(f"读取 feature_vector 数据失败: {str(e)}")
        raise

if __name__ == "__main__":
    df = extract_feature_vectors()
    print(f"读取完成，共 {len(df)} 条记录，有效记录 {df['is_valid'].sum()} 条")
    print(f"数据已保存到 feature_vectors.csv 和 feature_vectors_parsed.json")
    print(df.head())
    print(f"关键词数量: {len(top_keywords)}")
    print(f"前10个关键词: {top_keywords[:10]}")