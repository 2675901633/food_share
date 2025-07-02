#!/bin/bash

# 创建日志目录
mkdir -p /var/log/redis

# 启动所有Redis节点
for port in 7001 7002 7003 7004 7005 7006; do
    echo "启动Redis节点: $port"
    redis-server redis-cluster/$port/redis.conf
    sleep 1
done

# 等待节点启动
sleep 5

# 创建集群
echo "创建Redis集群..."
redis-cli --cluster create \
    127.0.0.1:7001 127.0.0.1:7002 127.0.0.1:7003 \
    127.0.0.1:7004 127.0.0.1:7005 127.0.0.1:7006 \
    --cluster-replicas 1 --cluster-yes

echo "Redis集群启动完成"
echo "集群节点:"
echo "  主节点: 127.0.0.1:7001, 127.0.0.1:7002, 127.0.0.1:7003"
echo "  从节点: 127.0.0.1:7004, 127.0.0.1:7005, 127.0.0.1:7006" 