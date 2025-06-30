package cn.kmbeast.config;

import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.List;

/**
 * Redis集群配置类
 * 支持Redis Cluster模式，提供高可用和水平扩展能力
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.redis.cluster-enabled", havingValue = "true")
public class RedisClusterConfig {

    @Value("${spring.redis.cluster.nodes}")
    private List<String> clusterNodes;

    @Value("${spring.redis.cluster.max-redirects:3}")
    private int maxRedirects;

    @Value("${spring.redis.timeout:3000ms}")
    private Duration timeout;

    @Value("${app.redis.health-check-interval:30}")
    private int healthCheckInterval;

    /**
     * Redis集群配置
     */
    @Bean
    @Primary
    public RedisClusterConfiguration redisClusterConfiguration() {
        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(clusterNodes);
        clusterConfig.setMaxRedirects(maxRedirects);
        
        log.info("Redis集群配置初始化完成，节点: {}, 最大重定向次数: {}", 
                clusterNodes, maxRedirects);
        return clusterConfig;
    }

    /**
     * Lettuce客户端配置
     * 配置集群拓扑刷新和连接选项
     */
    @Bean
    @Primary
    public LettuceClientConfiguration lettuceClientConfiguration() {
        // 集群拓扑刷新配置
        ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                // 开启自适应集群拓扑刷新
                .enableAdaptiveRefreshTrigger(
                        ClusterTopologyRefreshOptions.RefreshTrigger.MOVED_REDIRECT,
                        ClusterTopologyRefreshOptions.RefreshTrigger.ASK_REDIRECT
                )
                // 定时刷新集群拓扑
                .enablePeriodicRefresh(Duration.ofSeconds(healthCheckInterval))
                .build();

        // 集群客户端选项
        ClusterClientOptions clusterClientOptions = ClusterClientOptions.builder()
                .topologyRefreshOptions(topologyRefreshOptions)
                // 验证集群节点成员关系
                .validateClusterNodeMembership(false)
                .build();

        return LettuceClientConfiguration.builder()
                .clientOptions(clusterClientOptions)
                .commandTimeout(timeout)
                .build();
    }

    /**
     * Redis连接工厂
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory(
            RedisClusterConfiguration clusterConfig,
            LettuceClientConfiguration clientConfig) {
        
        LettuceConnectionFactory factory = new LettuceConnectionFactory(clusterConfig, clientConfig);
        factory.setValidateConnection(true);
        factory.afterPropertiesSet();
        
        log.info("Redis集群连接工厂初始化完成");
        return factory;
    }

    /**
     * Redis模板配置（集群版）
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> clusterRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 使用Jackson2JsonRedisSerializer来序列化和反序列化redis的value值
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = 
                new Jackson2JsonRedisSerializer<>(Object.class);

        // 使用StringRedisSerializer来序列化和反序列化redis的key值
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // key采用String的序列化方式
        template.setKeySerializer(stringRedisSerializer);
        // hash的key也采用String的序列化方式
        template.setHashKeySerializer(stringRedisSerializer);
        // value序列化方式采用jackson
        template.setValueSerializer(jackson2JsonRedisSerializer);
        // hash的value序列化方式采用jackson
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        
        log.info("Redis集群模板配置完成");
        return template;
    }
}
