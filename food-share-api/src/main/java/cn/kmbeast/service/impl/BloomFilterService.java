package cn.kmbeast.service.impl;

import cn.kmbeast.mapper.GourmetMapper;
import cn.kmbeast.pojo.Gourmet;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.charset.Charset;
import java.util.List;

/**
 * 布隆过滤器服务
 * 用于防止缓存穿透
 */
@Slf4j
@Service
public class BloomFilterService {

    @Autowired
    private GourmetMapper gourmetMapper;

    private BloomFilter<String> gourmetBloomFilter;

    @PostConstruct
    public void initBloomFilter() {
        log.info("初始化美食布隆过滤器...");
        
        // 创建布隆过滤器，预估100万个美食ID，误判率1%
        gourmetBloomFilter = BloomFilter.create(
            Funnels.stringFunnel(Charset.defaultCharset()), 
            1000000, 
            0.01
        );
        
        // 加载所有存在的美食ID
        loadExistingGourmetIds();
        
        log.info("美食布隆过滤器初始化完成");
    }

    /**
     * 检查美食ID是否可能存在
     */
    public boolean mightContainGourmet(Integer gourmetId) {
        if (gourmetId == null) return false;
        return gourmetBloomFilter.mightContain(gourmetId.toString());
    }

    /**
     * 添加美食ID到布隆过滤器
     */
    public void addGourmet(Integer gourmetId) {
        if (gourmetId != null) {
            gourmetBloomFilter.put(gourmetId.toString());
            log.debug("添加美食ID到布隆过滤器: {}", gourmetId);
        }
    }

    /**
     * 加载所有存在的美食ID
     */
    private void loadExistingGourmetIds() {
        try {
            List<Gourmet> allGourmets = gourmetMapper.selectAll();
            int count = 0;
            
            for (Gourmet gourmet : allGourmets) {
                gourmetBloomFilter.put(gourmet.getId().toString());
                count++;
            }
            
            log.info("加载{}个美食ID到布隆过滤器", count);
        } catch (Exception e) {
            log.error("加载美食ID到布隆过滤器失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 重新初始化布隆过滤器
     */
    public void reinitialize() {
        initBloomFilter();
    }
} 