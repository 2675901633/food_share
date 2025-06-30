import jieba
import logging
from pathlib import Path
from datetime import timedelta


class Config:
    # MySQL配置
    MYSQL_HOST = 'localhost'
    MYSQL_PORT = 3306
    MYSQL_USER = 'root'
    MYSQL_PASSWORD = 'sx2002411'
    MYSQL_DB = 'food_share'
    SQLALCHEMY_DATABASE_URI = f'mysql+pymysql://{MYSQL_USER}:{MYSQL_PASSWORD}@{MYSQL_HOST}:{MYSQL_PORT}/{MYSQL_DB}'
    SQLALCHEMY_TRACK_MODIFICATIONS = False

    # JWT配置（必须与Java的JwtUtil一致）
    JWT_SECRET = "d8c986df-8512-42b5-906f-eeea9b3acf86"
    JWT_ALGORITHM = "HS256"
    JWT_EXPIRE_DAYS = 7  # 有效期7天

    @staticmethod
    def init_jieba():
        """初始化分词器"""
        try:
            jieba.initialize()
            dict_path = Path('dict/stopwords.txt')
            if dict_path.exists():
                jieba.load_userdict(str(dict_path))
        except Exception as e:
            logging.error(f"Jieba初始化失败: {str(e)}")
            raise
