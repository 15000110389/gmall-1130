package com.atguigu.gmall.pms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import com.atguigu.gmall.pms.mapper.AttrMapper;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.sun.javafx.collections.MappingChange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;


@Service("skuAttrValueService")
public class SkuAttrValueServiceImpl extends ServiceImpl<SkuAttrValueMapper, SkuAttrValueEntity> implements SkuAttrValueService {
    @Resource
    private SkuMapper skuMapper;
    @Autowired
    private SkuAttrValueMapper attrValueMapper;
    @Override
    public List<SaleAttrValueVo> querySaleAttrValuesByspuId(Long spuId) {
//        List<SkuEntity> skuEntities = skuMapper.selectList(new QueryWrapper<SkuEntity>().eq("spu_id", spuId));
//        if (CollectionUtils.isEmpty(skuEntities)) {
//            return null;
//        }
//        List<Long> skuIds = skuEntities.stream().map(SkuEntity::getId).collect(Collectors.toList());
//        List<SkuAttrValueEntity> attrValueEntityList = this.list(new QueryWrapper<SkuAttrValueEntity>().eq("sku_id", skuIds).orderByDesc("attr_id"));

//        if (CollectionUtils.isEmpty(attrValueEntityList)){
//            return null;
//        }
//        List<SaleAttrValueVo> saleAttrValueVos = new ArrayList<>();
//        Map<Long, List<SkuAttrValueEntity>> map = attrValueEntityList.stream().collect(Collectors.groupingBy(SkuAttrValueEntity::getAttrId));
//        map.forEach((attrId,skuAttrValueEntityList)->{
//            SaleAttrValueVo saleAttrValueVo = new SaleAttrValueVo();
//            saleAttrValueVo.setAttrId(attrId);
//            saleAttrValueVo.setAttrName(skuAttrValueEntityList.get(0).getAttrName());
//            saleAttrValueVo.setAttrValue(skuAttrValueEntityList.stream().map(SkuAttrValueEntity::getAttrValue).collect(Collectors.toSet()));
//            saleAttrValueVos.add(saleAttrValueVo);
//        });
//        return saleAttrValueVos;
        // ??????spuId??????sku??????
        List<SkuEntity> skuEntities = this.skuMapper.selectList(new QueryWrapper<SkuEntity>().eq("spu_id", spuId));
        if (CollectionUtils.isEmpty(skuEntities)){
            return null;
        }
        // ??????skuId??????
        List<Long> skuIds = skuEntities.stream().map(SkuEntity::getId).collect(Collectors.toList());

        // ????????????????????????
        List<SkuAttrValueEntity> attrValueEntities = this.list(new QueryWrapper<SkuAttrValueEntity>().in("sku_id", skuIds).orderByAsc("attr_id"));
        if (CollectionUtils.isEmpty(attrValueEntities)){
            return null;
        }

        List<SaleAttrValueVo> saleAttrValueVos = new ArrayList<>();
        // ???attrId??????key????????????attrId?????????ListSkuAttrValueEntity>?????????
        Map<Long, List<SkuAttrValueEntity>> map = attrValueEntities.stream().collect(Collectors.groupingBy(SkuAttrValueEntity::getAttrId));
        map.forEach((attrId, skuAttrValueEntities) -> { // ???????????????k-v???????????????SaleAttrValueVo??????
            SaleAttrValueVo saleAttrValueVo = new SaleAttrValueVo();
            saleAttrValueVo.setAttrId(attrId);
            // groupby?????????????????????????????????????????????????????????
            saleAttrValueVo.setAttrName(skuAttrValueEntities.get(0).getAttrName());
            Set<String> attrValues = skuAttrValueEntities.stream().map(SkuAttrValueEntity::getAttrValue).collect(Collectors.toSet());
            saleAttrValueVo.setAttrValue(attrValues);
            saleAttrValueVos.add(saleAttrValueVo); // ????????????
        });
        return saleAttrValueVos;
    }

    @Override
    public String querySkuAttrValuesBySpuId(Long spuId) {
        // ??????spuId??????sku??????
        List<SkuEntity> skuEntities = this.skuMapper.selectList(new QueryWrapper<SkuEntity>().eq("spu_id", spuId));
        if (CollectionUtils.isEmpty(skuEntities)){
            return null;
        }
        // ??????skuId??????
        List<Long> skuIds = skuEntities.stream().map(SkuEntity::getId).collect(Collectors.toList());

        // ??????????????????
        List<Map<String, Object>> maps = this.attrValueMapper.querySkuAttrValuesBySpuId(skuIds);
        if (CollectionUtils.isEmpty(maps)){
            return null;
        }
        Map<String, Long> mapping = maps.stream().collect(Collectors.toMap(map -> map.get("attrValues").toString(), map -> (Long) map.get("sku_id")));
        return JSON.toJSONString(mapping);
    }



    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SkuAttrValueEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SkuAttrValueEntity>()
        );

        return new PageResultVo(page);
    }
    @Resource
    private AttrMapper attrMapper;

    @Override
    public List<SkuAttrValueEntity> querySearchAttrValuesByCidAndSkuId(Long cid, Long skuId) {
        // 1.???????????????id????????????????????????????????????
        List<AttrEntity> attrEntities = this.attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("category_id", cid).eq("search_type", 1));
        if (CollectionUtils.isEmpty(attrEntities)){
            return null;
        }
        // ??????attrIds
        List<Long> attrIds = attrEntities.stream().map(AttrEntity::getId).collect(Collectors.toList());

        // 2.??????skuId???AttrIds???????????????????????????????????????
        return this.list(new QueryWrapper<SkuAttrValueEntity>().eq("sku_id", skuId).in("attr_id", attrIds));
    }

}