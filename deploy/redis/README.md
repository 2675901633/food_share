# Redis集群与哨兵部署说明文档

本文档提供了在`food-share`项目中部署Redis集群（Cluster）和哨兵（Sentinel）的详细步骤。

## 1. 环境要求

- Redis版本: 6.0+
- JDK版本: 1.8+
- Spring Boot版本: 2.x+

## 2. 部署选择

根据您的需求，可以选择以下两种部署模式之一：

- **开发/测试环境**: Redis哨兵模式（主从+哨兵）
- **生产环境**: Redis集群模式（Cluster）

## 3. 哨兵模式部署（开发/测试环境）

### 3.1 部署步骤

1. 进入哨兵配置目录：
```bash
cd deploy/redis/sentinel
```

2. 执行启动脚本：
```bash
chmod +x start-redis-sentinel.sh
./start-redis-sentinel.sh
```

3. 验证哨兵状态：
```bash
redis-cli -p 26379 sentinel masters
```

### 3.2 使用哨兵模式启动应用

```bash
# 使用哨兵配置启动
java -jar food-share-api.jar --spring.profiles.active=sentinel
```

## 4. 集群模式部署（生产环境）

### 4.1 部署步骤

1. 进入集群配置目录：
```bash
cd deploy/redis/cluster
```

2. 生成集群配置：
```bash
chmod +x setup-redis-cluster.sh
./setup-redis-cluster.sh
```

3. 启动Redis集群：
```bash
chmod +x start-redis-cluster.sh
./start-redis-cluster.sh
```

4. 验证集群状态：
```bash
redis-cli -p 7001 cluster nodes
```

### 4.2 使用集群模式启动应用

```bash
# 使用集群配置启动
java -jar food-share-api.jar --spring.profiles.active=cluster
```

## 5. 监控与维护

### 5.1 使用监控脚本

```bash
cd deploy/redis
chmod +x monitor-redis.sh
./monitor-redis.sh
```

### 5.2 健康检查API

应用启动后，可以通过以下API检查Redis连接状态：

```bash
curl http://localhost:21090/api/food-share-sys/v1.0/redis/health/check
```

## 6. 故障排查

### 6.1 哨兵模式故障排查

- **检查主从状态**：
```bash
redis-cli -p 6379 info replication
```

- **查看哨兵日志**：
```bash
tail -f /var/log/redis/sentinel-*.log
```

### 6.2 集群模式故障排查

- **检查集群状态**：
```bash
redis-cli -p 7001 cluster info
```

- **检查槽分配**：
```bash
redis-cli -p 7001 cluster slots
```

## 7. 注意事项

1. 确保Redis版本一致性：所有节点使用相同版本的Redis
2. 集群模式下，某些Redis命令有限制，特别是跨槽命令
3. 使用哈希标签`{}`确保相关key在同一个哈希槽中
4. 定期备份Redis数据
5. 监控Redis内存使用情况，避免触发内存限制

## 8. 附录：常用命令

### 8.1 哨兵常用命令

```bash
# 查看哨兵监控的主节点
redis-cli -p 26379 sentinel masters

# 查看从节点
redis-cli -p 26379 sentinel slaves mymaster

# 手动触发故障转移
redis-cli -p 26379 sentinel failover mymaster
```

### 8.2 集群常用命令

```bash
# 查看集群节点
redis-cli -p 7001 cluster nodes

# 检查集群健康状态
redis-cli -p 7001 cluster info

# 重新分片
redis-cli --cluster reshard 127.0.0.1:7001

# 添加节点
redis-cli --cluster add-node 127.0.0.1:7007 127.0.0.1:7001
``` 