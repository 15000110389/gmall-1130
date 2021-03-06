package com.atguigu.gmall.search.listener;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValue;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GoodsListener {
    @Resource
    private GmallPmsClient pmsClient;
    @Resource
    private GmallWmsClient wmsClient;
    @Resource
    private GoodsRepository goodsRepository;
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("SEARCH_INSERT_QUEUE"),
            exchange = @Exchange(value = "PMS_ITEM_EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"item.insert"}
    ))
    public void listener(Long spuId, Channel channel, Message message) throws IOException {
        if(spuId == null){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }
        ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsClient.querySpuById(spuId);
        SpuEntity spuEntity = spuEntityResponseVo.getData();
        if (spuEntity == null) {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }
        ResponseVo<List<SkuEntity>> skuList = this.pmsClient.querySkuList(spuId);
        List<SkuEntity> skuEntities = skuList.getData();
        if (!CollectionUtils.isEmpty(skuEntities)) {
            ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(spuEntity.getBrandId());
            BrandEntity brandEntity = brandEntityResponseVo.getData();
            ResponseVo<CategoryEntity> categoryEntityResponseVo = this.pmsClient.queryCategoryById(spuEntity.getCategoryId());
            CategoryEntity categoryEntity = categoryEntityResponseVo.getData();
            List<Goods> goodsList = skuEntities.stream().map(skuEntity -> {
                Goods goods = new Goods();
                // sku????????????
                goods.setSkuId(skuEntity.getId());
                goods.setTitle(skuEntity.getTitle());
                goods.setSubTitle(skuEntity.getSubtitle());
                goods.setDefaultImage(skuEntity.getDefaultImage());
                goods.setPrice(skuEntity.getPrice().doubleValue());

                // ????????????
                goods.setCreateTime(spuEntity.getCreateTime());
                ResponseVo<List<WareSkuEntity>> wareSkuList = this.wmsClient.queryWareSkuList(skuEntity.getId());
                List<WareSkuEntity> wareSkuListData = wareSkuList.getData();
                if (!CollectionUtils.isEmpty(wareSkuListData)){
                    goods.setStore( wareSkuListData.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock()-wareSkuEntity.getStockLocked()>0));
                    goods.setSales(wareSkuListData.stream().map(WareSkuEntity::getSales).reduce((a,b)->a+b).get());
                }

                // ???????????????????????????
                if (brandEntity != null) {
                    goods.setBrandId(brandEntity.getId());
                    goods.setBrandName(brandEntity.getName());
                    goods.setLogo(brandEntity.getLogo());
                }
                if (categoryEntity != null) {
                    goods.setCategoryId(categoryEntity.getId());
                    goods.setCategoryName(categoryEntity.getName());
                }


                // ?????????????????????????????????
                List<SearchAttrValue> searchAttrValues = new ArrayList<>();
                // ???????????????????????????
                ResponseVo<List<SkuAttrValueEntity>> skuSearchAttrValueResponseVo = this.pmsClient.querySearchAttrValuesByCidAndSkuId(skuEntity.getCategoryId(), skuEntity.getId());

                List<SkuAttrValueEntity> skuAttrValueEntities = skuSearchAttrValueResponseVo.getData();
                if (!CollectionUtils.isEmpty(skuAttrValueEntities)){
                    searchAttrValues.addAll(skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                        SearchAttrValue searchAttrValue = new SearchAttrValue();
                        BeanUtils.copyProperties(skuAttrValueEntity, searchAttrValue);
                        return searchAttrValue;
                    }).collect(Collectors.toList()));
                }
                // ???????????????????????????
                ResponseVo<List<SpuAttrValueEntity>> spuSearchAttrValueResponseVo = this.pmsClient.querySearchAttrValuesByCidAndSpuId(skuEntity.getCategoryId(), spuEntity.getId());
                List<SpuAttrValueEntity> spuAttrValueEntities = spuSearchAttrValueResponseVo.getData();
                if (!CollectionUtils.isEmpty(spuAttrValueEntities)){
                    searchAttrValues.addAll(spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                        SearchAttrValue searchAttrValue = new SearchAttrValue();
                        BeanUtils.copyProperties(spuAttrValueEntity, searchAttrValue);
                        return searchAttrValue;
                    }).collect(Collectors.toList()));
                }

                goods.setSearchAttrs(searchAttrValues);

                return goods;
            }).collect(Collectors.toList());

            this.goodsRepository.saveAll(goodsList);
        }

        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

}
