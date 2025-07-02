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
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.List;

/**
 * Redis哨兵配置类
 * 支持Redis Sentinel模式，提供高可用性
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.redis.sentinel-enabled", havingValue = "true")
public class RedisSentinelConfig {

    @Value("${spring.redis.sentinel.master}")
    private String masterName;

    @Value("${spring.redis.sentinel.nodes}")
    private List<String> sentinelNodes;

    @Value("${spring.redis.timeout:3000ms}")
    private Duration timeout;

    @Value("${spring.redis.database:0}")
    private int database;

    @Value("${spring.redis.password:}")
    private String password;

    /**
     * Redis哨兵配置
     */
    @Bean
    @Primary
    public RedisSentinelConfiguration redisSentinelConfiguration() {
        RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration();

        // 设置主节点名称
        sentinelConfig.setMaster(masterName);

        // 添加哨兵节点
        for (String node : sentinelNodes) {
            String[] parts = node.split(":");
            sentinelConfig.sentinel(parts[0], Integer.parseInt(parts[1]));
        }

        // 设置数据库
        sentinelConfig.setDatabase(database);

        // 设置密码（如果有）
        if (password != null && !password.trim().isEmpty()) {
            sentinelConfig.setPassword(password);
        }

        log.info("Redis哨兵配置初始化完成，主节点: {}, 哨兵节点: {}", masterName, sentinelNodes);
        return sentinelConfig;
    }

    /**
     * Redis连接工厂
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory(RedisSentinelConfiguration sentinelConfig) {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(sentinelConfig);
        factory.setValidateConnection(true);
        factory.afterPropertiesSet();

        log.info("Redis哨兵连接工厂初始化完成");
        return factory;
    }

    /**
     * Redis模板配置（哨兵版）
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> sentinelRedisTemplate(RedisConnectionFactory connectionFactory) {
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

        log.info("Redis哨兵模板配置完成");
        return template;
    }
} 