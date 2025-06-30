from flask import Flask, jsonify, request
from recommand import Recommend, RecommendationResponse
from dataload import Dataload
import logging
from flask_cors import CORS
import jwt
import base64
import time
from config import Config

app = Flask(__name__)
CORS(app,
     resources={
         r"/*": {
             "origins": ["http://localhost:21091", "http://localhost:5000"],
             "methods": ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
             "allow_headers": ["Content-Type", "Authorization", "token"],
             "supports_credentials": True,
             "expose_headers": ["Content-Type", "Authorization"],
             "max_age": 3600
         }
     })

logging.basicConfig(level=logging.INFO,
                    format='%(asctime)s - %(levelname)s - %(message)s',
                    filename='recommendation_test.log',
                    encoding='utf-8')


def get_user_id_from_token():
    token = request.headers.get('token')
    if not token:
        # 开发模式，如果没有token，使用默认用户ID
        logging.warning("没有token，使用默认用户ID")
        return 1

    try:
        header = jwt.get_unverified_header(token)
        if not header or header.get(
                'alg') != Config.JWT_ALGORITHM or header.get('typ') != 'JWT':
            raise ValueError("无效的token: header校验失败")

        secret_key_bytes = base64.b64decode(Config.JWT_SECRET)
        payload = jwt.decode(token,
                             secret_key_bytes,
                             algorithms=[Config.JWT_ALGORITHM])

        required_claims = ['id', 'role']
        for claim in required_claims:
            if claim not in payload:
                raise ValueError(f"无效的token: 缺少{claim}字段")

        user_id = payload.get('id')
        if not isinstance(user_id, int):
            raise ValueError("无效的token: 用户ID格式错误")

        return user_id
    except jwt.ExpiredSignatureError:
        raise ValueError("token已过期")
    except jwt.InvalidTokenError as e:
        raise ValueError(f"无效的token: {str(e)}")
    except Exception as e:
        # 任何错误情况下，使用默认用户
        logging.error(f"Token解析错误: {str(e)}，使用默认用户ID")
        return 1


def evaluate_global_metrics(top_n: int = 10) -> tuple[float, float]:
    try:
        interaction_df = Dataload.load_interaction_data()
        gourmet_df = Dataload.load_gourmet_data()

        # 获取所有用户
        all_users = interaction_df['user_id'].unique()
        total_precision = 0.0
        total_recall = 0.0
        total_adjusted_recall = 0.0
        valid_users = 0

        for user_id in all_users:
            # 用户特定正样本
            positive_items = set(interaction_df[
                interaction_df['user_id'] == user_id]['content_id'].unique())
            if not positive_items:
                continue

            # 添加超时控制
            start_time = time.time()
            max_time = 2.0  # 最多2秒

            try:
                response = Recommend.recommend(user_id, top_n=top_n)

                if time.time() - start_time > max_time:
                    logging.warning(f"用户 {user_id} 推荐生成超时")
                    continue

                if not response.recommended_gourmets:
                    continue

                recommended_ids = set(
                    int(gourmet['id'])
                    for gourmet in response.recommended_gourmets)
                hits = len(recommended_ids & positive_items)

                # 计算每个用户的准确率和召回率
                user_precision = hits / len(
                    recommended_ids) if recommended_ids else 0.0
                user_recall = hits / len(
                    positive_items) if positive_items else 0.0

                # 计算调整后的召回率
                interaction_count = len(positive_items)
                if interaction_count <= 3:
                    expected_hits = max(1, interaction_count * 0.3)
                elif interaction_count <= 10:
                    expected_hits = max(1, interaction_count * 0.4)
                else:
                    expected_hits = max(2, interaction_count * 0.5)

                user_adjusted_recall = min(
                    1.0, hits / expected_hits) if expected_hits > 0 else 0.0

                total_precision += user_precision
                total_recall += user_recall
                total_adjusted_recall += user_adjusted_recall
                valid_users += 1

            except Exception as e:
                logging.error(f"用户 {user_id} 推荐生成错误: {str(e)}")
                continue

        # 计算平均指标
        avg_precision = total_precision / valid_users if valid_users > 0 else 0.0
        avg_adjusted_recall = total_adjusted_recall / valid_users if valid_users > 0 else 0.0

        logging.info(f"全局评估 - 有效用户数: {valid_users}")
        logging.info(f"全局评估 - 平均准确率: {avg_precision:.4f}")
        logging.info(f"全局评估 - 平均调整召回率: {avg_adjusted_recall:.4f}")

        return avg_precision, avg_adjusted_recall
    except Exception as e:
        logging.error(f"全局评估错误: {str(e)}")
        return 0.0, 0.0


@app.route('/recommend/evaluate')
def evaluate_metrics_api():
    try:
        user_id = get_user_id_from_token()
        top_n = 10

        # 当前用户评估
        user_precision, user_adjusted_recall = evaluate_metrics(user_id, top_n)

        # 全局用户评估
        avg_precision, avg_adjusted_recall = evaluate_global_metrics(top_n)

        return jsonify({
            "code": 200,
            "message": "评估完成",
            "data": {
                "current_user": {
                    "user_id": user_id,
                    "precision": f"{user_precision:.4f}",
                    "recall": f"{user_adjusted_recall:.4f}"
                },
                "global_metrics": {
                    "precision": f"{avg_precision:.4f}",
                    "adjusted_recall": f"{avg_adjusted_recall:.4f}"
                }
            }
        })
    except ValueError as e:
        logging.error(f"评估错误: {str(e)}")
        return jsonify({"code": 401, "message": str(e), "data": None}), 401
    except Exception as e:
        logging.error(f"评估错误: {str(e)}")
        return jsonify({
            "code": 500,
            "message": f"评估错误: {str(e)}",
            "data": None
        }), 500


@app.route('/recommend/list')
def get_recommendation_list():
    try:
        user_id = get_user_id_from_token()
        top_n = 10

        # 添加超时控制
        start_time = time.time()
        max_time = 5.0  # 最多5秒

        try:
            response = Recommend.recommend(user_id, top_n=top_n)

            # 检查推荐是否超时
            if time.time() - start_time > max_time:
                logging.warning("推荐生成超时，使用备选方案")
                # 简单备选方案：返回一个空的推荐列表
                return jsonify({
                    "code": 200,
                    "message": "推荐生成成功(备选)",
                    "data": {
                        "user_id": user_id,
                        "recommended_gourmets": [],
                        "recommended_categories": []
                    }
                })

            return jsonify({
                "code": 200,
                "message": "推荐生成成功",
                "data": {
                    "user_id": response.user_id,
                    "recommended_gourmets": response.recommended_gourmets,
                    "recommended_categories": response.recommended_categories
                }
            })
        except Exception as e:
            logging.error(f"推荐生成错误: {str(e)}")
            # 发生错误时返回空列表
            return jsonify({
                "code": 200,
                "message": "推荐生成成功(备选)",
                "data": {
                    "user_id": user_id,
                    "recommended_gourmets": [],
                    "recommended_categories": []
                }
            })
    except ValueError as e:
        logging.error(f"推荐生成错误: {str(e)}")
        return jsonify({"code": 401, "message": str(e), "data": None}), 401
    except Exception as e:
        logging.error(f"推荐生成错误: {str(e)}")
        return jsonify({
            "code": 500,
            "message": f"推荐生成错误: {str(e)}",
            "data": None
        }), 500


# 保留第一个文件的评估函数
def evaluate_metrics(user_id: int, top_n: int = 10) -> tuple[float, float]:
    try:
        interaction_df = Dataload.load_interaction_data()
        gourmet_df = Dataload.load_gourmet_data()
        positive_items = set(interaction_df[interaction_df['user_id'] ==
                                            user_id]['content_id'].unique())
        negative_items = set(gourmet_df['id'].unique()) - positive_items
        logging.info(
            f"用户 {user_id} 的正样本数: {len(positive_items)}，负样本数: {len(negative_items)}"
        )

        # 添加超时控制
        start_time = time.time()
        max_time = 2.0  # 最多2秒

        try:
            response = Recommend.recommend(user_id, top_n=top_n)

            # 检查推荐是否超时
            if time.time() - start_time > max_time:
                logging.warning(f"用户 {user_id} 评估超时")
                return 0.0, 0.0

            recommended_gourmets = response.recommended_gourmets

            if not recommended_gourmets:
                logging.warning(f"用户 {user_id} 无推荐，准确率=0，召回率=0")
                return 0.0, 0.0

            recommended_ids = set(
                int(gourmet['id']) for gourmet in recommended_gourmets)
            hits = len(recommended_ids & positive_items)
            precision = hits / len(
                recommended_gourmets) if recommended_gourmets else 0.0

            interaction_count = len(positive_items)
            if interaction_count <= 3:
                expected_hits = max(1, interaction_count * 0.3)
            elif interaction_count <= 10:
                expected_hits = max(1, interaction_count * 0.4)
            else:
                expected_hits = max(2, interaction_count * 0.5)

            adjusted_recall = min(1.0, hits /
                                  expected_hits) if expected_hits > 0 else 0.0

            logging.info(
                f"用户 {user_id} 的准确率: {hits}/{len(recommended_gourmets)} = {precision:.4f}"
            )
            logging.info(
                f"用户 {user_id} 的调整后召回率: {hits}/{expected_hits:.1f} = {adjusted_recall:.4f}"
            )

            return precision, adjusted_recall

        except Exception as e:
            logging.error(f"用户评估错误: {str(e)}")
            return 0.0, 0.0

    except Exception as e:
        logging.error(f"用户 {user_id} 的评估错误: {str(e)}")
        return 0.0, 0.0


if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5000)
