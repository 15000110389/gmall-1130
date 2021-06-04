package com.atguigu.gmall.sms.service.impl;

import com.atguigu.gmall.sms.entity.SkuFullReductionEntity;
import com.atguigu.gmall.sms.entity.SkuLadderEntity;
import com.atguigu.gmall.sms.mapper.SkuFullReductionMapper;
import com.atguigu.gmall.sms.mapper.SkuLadderMapper;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.sms.mapper.SkuBoundsMapper;
import com.atguigu.gmall.sms.entity.SkuBoundsEntity;
import com.atguigu.gmall.sms.service.SkuBoundsService;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;


@Service("skuBoundsService")
public class SkuBoundsServiceImpl extends ServiceImpl<SkuBoundsMapper, SkuBoundsEntity> implements SkuBoundsService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SkuBoundsEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SkuBoundsEntity>()
        );

        return new PageResultVo(page);
    }
    @Resource
    private SkuFullReductionMapper skuFullReductionMapper;
    @Resource
    private SkuLadderMapper skuLadderMapper;
    @Transactional
    @Override
    public void saveSales(SkuSaleVo skuSaleVo) {
        SkuBoundsEntity skuBoundsEntity = new SkuBoundsEntity();
        BeanUtils.copyProperties(skuSaleVo, skuBoundsEntity);
        List<Integer> work = skuSaleVo.getWork();
        if (work!=null&& work.size()==4){
            skuBoundsEntity.setWork(work.get(3)*8+work.get(2)*4+work.get(1*2)+work.get(0));
        }
        this.save(skuBoundsEntity);


        SkuFullReductionEntity skuFullReductionEntity = new SkuFullReductionEntity();
        BeanUtils.copyProperties(skuSaleVo, skuFullReductionEntity);
        skuFullReductionEntity.setAddOther(skuSaleVo.getFullAddOther());
        this.skuFullReductionMapper.insert(skuFullReductionEntity);

        SkuLadderEntity skuLadderEntity = new SkuLadderEntity();
        BeanUtils.copyProperties(skuSaleVo,skuLadderEntity);
        skuLadderEntity.setAddOther(skuSaleVo.getLadderAddOther());
        this.skuLadderMapper.insert(skuLadderEntity);


    }

    @Override
    public List<ItemSaleVo> querySaleListBySkuId(Long skuId) {
        List<ItemSaleVo> itemSaleVos = new ArrayList<>();
        // 查询积分
        SkuBoundsEntity skuBoundsEntity = this.getOne(new QueryWrapper<SkuBoundsEntity>().eq("sku_id", skuId));
        if (skuBoundsEntity != null) {
            ItemSaleVo itemSaleVo = new ItemSaleVo();
            itemSaleVo.setType("积分");
            itemSaleVo.setDesc("送" + skuBoundsEntity.getGrowBounds() + "成长积分，送" + skuBoundsEntity.getBuyBounds() + "购物积分");
            itemSaleVos.add(itemSaleVo);
        }

        // 查询打折
        SkuLadderEntity ladderEntity = this.skuLadderMapper.selectOne(new QueryWrapper<SkuLadderEntity>().eq("sku_id", skuId));
        if (ladderEntity != null) {
            ItemSaleVo itemSaleVo = new ItemSaleVo();
            itemSaleVo.setType("打折");
            itemSaleVo.setDesc("满" + ladderEntity.getFullCount() + "件打" + ladderEntity.getDiscount().divide(new BigDecimal(10)) + "折");
            itemSaleVos.add(itemSaleVo);
        }

        // 查询满减
        SkuFullReductionEntity reductionEntity = this.skuFullReductionMapper.selectOne(new QueryWrapper<SkuFullReductionEntity>().eq("sku_id", skuId));
        if (reductionEntity != null) {
            ItemSaleVo itemSaleVo = new ItemSaleVo();
            itemSaleVo.setType("满减");
            itemSaleVo.setDesc("满" + reductionEntity.getFullPrice() + "减" + reductionEntity.getReducePrice());
            itemSaleVos.add(itemSaleVo);
        }
        return itemSaleVos;
    }

}