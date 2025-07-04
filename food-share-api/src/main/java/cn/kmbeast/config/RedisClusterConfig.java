package cn.kmbeast.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
 * 支持Redis Cluster模式，提供水平扩展和高可用性
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.redis.cluster-enabled", havingValue = "true")
public class RedisClusterConfig {

    @Value("${spring.redis.cluster.nodes}")
    private List<String> clusterNodes;

    @Value("${spring.redis.cluster.max-redirects:3}")
    private Integer maxRedirects;

    @Value("${spring.redis.timeout:3000ms}")
    private Duration timeout;

    @Value("${spring.redis.password:}")
    private String password;

    /**
     * Redis集群配置
     */
    @Bean
    @Primary
    public RedisClusterConfiguration redisClusterConfiguration() {
        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(clusterNodes);
        
        // 设置最大重定向次数
        clusterConfig.setMaxRedirects(maxRedirects);
        
        // 设置密码（如果有）
        if (password != null && !password.trim().isEmpty()) {
            clusterConfig.setPassword(password);
        }
        
        log.info("Redis集群配置初始化完成，节点: {}, 最大重定向: {}", clusterNodes, maxRedirects);
        return clusterConfig;
    }

    /**
     * Redis集群连接工厂
     */
    @Bean
    @Primary
    public RedisConnectionFactory clusterRedisConnectionFactory(RedisClusterConfiguration clusterConfig) {
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(timeout)
                .readFrom(io.lettuce.core.ReadFrom.REPLICA_PREFERRED) // 优先从从节点读取
                .build();
        
        LettuceConnectionFactory factory = new LettuceConnectionFactory(clusterConfig, clientConfig);
        factory.afterPropertiesSet();
        
        log.info("Redis集群连接工厂初始化完成");
        return factory;
    }

    /**
     * Redis集群模板配置
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> clusterRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 使用Jackson2JsonRedisSerializer来序列化和反序列化redis的value值
        Jackson2JsonRedisSerializer<Object> jacksonSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        mapper.registerModule(new JavaTimeModule());
        jacksonSerializer.setObjectMapper(mapper);

        // 使用StringRedisSerializer来序列化和反序列化redis的key值
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // key采用String的序列化方式
        template.setKeySerializer(stringRedisSerializer);
        // hash的key也采用String的序列化方式
        template.setHashKeySerializer(stringRedisSerializer);
        // value序列化方式采用jackson
        template.setValueSerializer(jacksonSerializer);
        // hash的value序列化方式采用jackson
        template.setHashValueSerializer(jacksonSerializer);

        template.afterPropertiesSet();

        log.info("Redis集群模板配置完成");
        return template;
    }
}
