package com.atguigu.gmall.pms.mapper;

import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * sku销售属性&值
 * 
 * @author fengge
 * @email fengge@atguigu.com
 * @date 2021-05-13 19:42:58
 */
@Mapper
public interface SkuAttrValueMapper extends BaseMapper<SkuAttrValueEntity> {

   List<Map<String, Object>> querySkuAttrValuesBySpuId(@Param("skuIds") List<Long> skuIds);
}
