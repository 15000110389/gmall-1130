package com.atguigu.gmallsearch.repository;

import com.atguigu.gmallsearch.pojo.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface GoodsRepository extends ElasticsearchRepository<Goods, Long>{
}
