package com.atguigu.gmall.index.config;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.sun.org.apache.regexp.internal.RE;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Configuration
public class BloomFilterConfig {
    @Autowired
    private RedissonClient redissonClient;
    private static final String KEY_PREFIX="index:cates";
    @Autowired
    private GmallPmsClient pmsClient;
    @Bean
    public RBloomFilter rBloomFilter(){
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter("index:bloom");
        bloomFilter.tryInit(500,0.03);
        ResponseVo<List<CategoryEntity>> responseVo = pmsClient.queryCategoryList(0l);
        List<CategoryEntity> data = responseVo.getData();
        if (!CollectionUtils.isEmpty(data)) {
            data.forEach(da->{
                bloomFilter.add(KEY_PREFIX+da.getId());
            });
        }
        return bloomFilter;
    }
}
