package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import com.atguigu.gmall.pms.mapper.AttrMapper;
import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.mapper.SpuAttrValueMapper;
import com.atguigu.gmall.pms.vo.AttrValueVo;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.AttrGroupMapper;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.gmall.pms.service.AttrGroupService;
import org.springframework.util.CollectionUtils;
import org.w3c.dom.Attr;

import javax.annotation.Resource;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupMapper, AttrGroupEntity> implements AttrGroupService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<AttrGroupEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageResultVo(page);
    }
    @Resource
    private AttrMapper attrMapper;
    @Override
    public List<AttrGroupEntity> queryAttrGroupListByCatId(Long catId) {
        List<AttrGroupEntity> groupEntityList = this.list(new QueryWrapper<AttrGroupEntity>().eq("category_id", catId));
        if (CollectionUtils.isEmpty(groupEntityList)){
            return null;
        }
        groupEntityList.forEach(group->{
            List<AttrEntity> attrEntityList = this.attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("group_id", group.getId()).eq("type",1));
            group.setAttrEntities(attrEntityList);
        });
        return groupEntityList;
    }
    @Resource
    private SpuAttrValueMapper spuAttrValueMapper;
    @Resource
    private SkuAttrValueMapper skuAttrValueMapper;
    @Override
    public List<ItemGroupVo> queryGroupListByCid(Long cid, Long spuId, Long skuId) {
//        List<AttrGroupEntity> categoryId = this.list(new QueryWrapper<AttrGroupEntity>().eq(("category_id"),cid));
//        if (CollectionUtils.isEmpty(categoryId)){
//            return null;
//        }
//        return categoryId.stream().map(attrGroupEntity -> {
//            ItemGroupVo itemGroupVo = new ItemGroupVo();
//            itemGroupVo.setGroupName(attrGroupEntity.getName());
//            List<AttrEntity> attrEntities = attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("group_id", attrGroupEntity.getId()));
//            if (CollectionUtils.isEmpty(attrEntities)){
//                return itemGroupVo;
//            }
//            List<AttrValueVo> attrValueVos=new ArrayList();
//            List<Long> attrIds = attrEntities.stream().map(AttrEntity::getId).collect(Collectors.toList());
//            List<SpuAttrValueEntity> spuAttrValueEntities = spuAttrValueMapper.selectList(new QueryWrapper<SpuAttrValueEntity>().eq("spu_id", spuId).in("attr_id", attrIds));
//            if (CollectionUtils.isEmpty(spuAttrValueEntities)){
//                attrValueVos.addAll(spuAttrValueEntities.stream().map(spuAttrValueEntity->{
//                    AttrValueVo attrValueVo = new AttrValueVo();
//                    BeanUtils.copyProperties(spuAttrValueEntity, attrValueVo);
//                    return attrValueVo;
//                }).collect(Collectors.toList()));
//            }

//            List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrValueMapper.selectList(new QueryWrapper<SkuAttrValueEntity>().eq("sku_id", skuId).in("attr_id", attrIds));
//            if (CollectionUtils.isEmpty(skuAttrValueEntities)){
//                attrValueVos.addAll(skuAttrValueEntities.stream().map(skuAttrValueEntitie->{
//                    AttrValueVo attrValueVo = new AttrValueVo();
//                    BeanUtils.copyProperties(skuAttrValueEntitie, attrValueVo);
//                    return attrValueVo;
//                }).collect(Collectors.toList()));
//            }
//            itemGroupVo.setAttrs(attrValueVos);
//            return itemGroupVo;
//        }).collect(Collectors.toList());
        // ??????cid????????????????????????
        List<AttrGroupEntity> groupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("category_id", cid));
        if (CollectionUtils.isEmpty(groupEntities)){
            return null;
        }

        return groupEntities.stream().map(attrGroupEntity -> {
            ItemGroupVo itemGroupVo = new ItemGroupVo();
            itemGroupVo.setGroupName(attrGroupEntity.getName());
            // ????????????????????????id?????????????????????
            List<AttrEntity> attrEntities = this.attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("group_id", attrGroupEntity.getId()));
            if (CollectionUtils.isEmpty(attrEntities)){
                return itemGroupVo;
            }
            // ??????????????????id??????
            List<Long> attrIds = attrEntities.stream().map(AttrEntity::getId).collect(Collectors.toList());

            // ??????????????????????????????spu sku???
            List<AttrValueVo> attrValueVos = new ArrayList<>();
            List<SpuAttrValueEntity> spuAttrValueEntities = this.spuAttrValueMapper.selectList(new QueryWrapper<SpuAttrValueEntity>().eq("spu_id", spuId).in("attr_id", attrIds));
            if(!CollectionUtils.isEmpty(spuAttrValueEntities)){
                attrValueVos.addAll(spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                    AttrValueVo attrValueVo = new AttrValueVo();
                    BeanUtils.copyProperties(spuAttrValueEntity, attrValueVo);
                    return attrValueVo;
                }).collect(Collectors.toList()));
            }

            List<SkuAttrValueEntity> skuAttrValueEntities = this.skuAttrValueMapper.selectList(new QueryWrapper<SkuAttrValueEntity>().eq("sku_id", skuId).in("attr_id", attrIds));
            if(!CollectionUtils.isEmpty(skuAttrValueEntities)){
                attrValueVos.addAll(skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                    AttrValueVo attrValueVo = new AttrValueVo();
                    BeanUtils.copyProperties(skuAttrValueEntity, attrValueVo);
                    return attrValueVo;
                }).collect(Collectors.toList()));
            }

            itemGroupVo.setAttrs(attrValueVos);

            return itemGroupVo;
        }).collect(Collectors.toList());
    }

}