#!/bin/bash

# 创建集群目录
mkdir -p redis-cluster/{7001,7002,7003,7004,7005,7006}

# 生成集群节点配置文件
for port in 7001 7002 7003 7004 7005 7006; do
cat > redis-cluster/$port/redis.conf << EOF
# 基础配置
port $port
bind 127.0.0.1
daemonize yes
pidfile /var/run/redis_$port.pid
logfile /var/log/redis/redis-$port.log
dir ./redis-cluster/$port

# 集群配置
cluster-enabled yes
cluster-config-file nodes-$port.conf
cluster-node-timeout 5000
cluster-announce-ip 127.0.0.1
cluster-announce-port $port

# 持久化配置
save 900 1
save 300 10
save 60 10000
rdbcompression yes
dbfilename dump.rdb
appendonly yes
appendfilename "appendonly.aof"

# 内存配置
maxmemory 1gb
maxmemory-policy allkeys-lru

# 安全配置
# requirepass your_password
# masterauth your_password
EOF
done

echo "Redis集群配置文件生成完成" 