#!/bin/bash

# 创建必要的目录
mkdir -p /var/log/redis
mkdir -p /var/lib/redis/{master,slave1,slave2,sentinel1,sentinel2,sentinel3}

# 启动Redis主节点
redis-server redis-master.conf

# 启动Redis从节点
redis-server redis-slave-6380.conf
redis-server redis-slave-6381.conf

# 等待Redis节点启动
sleep 3

# 启动哨兵
redis-sentinel sentinel-26379.conf
redis-sentinel sentinel-26380.conf
redis-sentinel sentinel-26381.conf

echo "Redis哨兵集群启动完成"
echo "主节点: 127.0.0.1:6379"
echo "从节点: 127.0.0.1:6380, 127.0.0.1:6381"
echo "哨兵: 127.0.0.1:26379, 127.0.0.1:26380, 127.0.0.1:26381" 