#!/bin/bash

echo "=== Redis服务状态 ==="
ps aux | grep redis | grep -v grep

echo "=== Redis连接数 ==="
redis-cli info clients | grep connected_clients

echo "=== 内存使用情况 ==="
redis-cli info memory | grep used_memory_human

echo "=== 集群状态 (如果是集群模式) ==="
redis-cli -p 7001 cluster info 2>/dev/null || echo "非集群模式"

echo "=== 哨兵状态 (如果是哨兵模式) ==="
redis-cli -p 26379 sentinel masters 2>/dev/null || echo "非哨兵模式" 