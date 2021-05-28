package com.atguigu.gmallsearch;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmallsearch.feign.GmallPmsClient;
import com.atguigu.gmallsearch.feign.GmallWmsClient;
import com.atguigu.gmallsearch.pojo.Goods;
import com.atguigu.gmallsearch.pojo.SearchAttrValue;
import com.atguigu.gmallsearch.repository.GoodsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
class GmallSearchApplicationTests {
    @Resource
    GoodsRepository repository;
    @Resource
    ElasticsearchRestTemplate restTemplate;
    @Resource
    GmallPmsClient pmsClient;
    @Resource
    GmallWmsClient wmsClient;
    @Test
    void contextLoads() {
        restTemplate.deleteIndex(Goods.class);
        restTemplate.createIndex(Goods.class);
        restTemplate.putMapping(Goods.class);
        Integer pageNum=1;
        Integer pageSize=100;
       do {
           ResponseVo<List<SpuEntity>> spuResponseVo = this.pmsClient.querySpuByPageJson(new PageParamVo(pageNum, pageSize, null));
           List<SpuEntity> spuEntities = spuResponseVo.getData();
           if (CollectionUtils.isEmpty(spuEntities)){
               return;
           }
            spuEntities.forEach(spuEntity -> {
                ResponseVo<List<SkuEntity>> skuList = this.pmsClient.querySkuList(spuEntity.getId());
                List<SkuEntity> skuEntities = skuList.getData();
                if (!CollectionUtils.isEmpty(skuEntities)) {
                    ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(spuEntity.getBrandId());
                    BrandEntity brandEntity = brandEntityResponseVo.getData();
                    ResponseVo<CategoryEntity> categoryEntityResponseVo = this.pmsClient.queryCategoryById(spuEntity.getCategoryId());
                    CategoryEntity categoryEntity = categoryEntityResponseVo.getData();
                    List<Goods> goodsList = skuEntities.stream().map(skuEntity -> {
                        Goods goods = new Goods();
                        // sku相关信息
                        goods.setSkuId(skuEntity.getId());
                        goods.setTitle(skuEntity.getTitle());
                        goods.setSubTitle(skuEntity.getSubtitle());
                        goods.setDefaultImage(skuEntity.getDefaultImage());
                        goods.setPrice(skuEntity.getPrice().doubleValue());

                        // 创建时间
                        goods.setCreateTime(spuEntity.getCreateTime());
                        ResponseVo<List<WareSkuEntity>> wareSkuList = this.wmsClient.queryWareSkuList(skuEntity.getId());
                        List<WareSkuEntity> wareSkuListData = wareSkuList.getData();
                        if (!CollectionUtils.isEmpty(wareSkuListData)){
                            goods.setStore( wareSkuListData.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock()-wareSkuEntity.getStockLocked()>0));
                            goods.setSales(wareSkuListData.stream().map(WareSkuEntity::getSales).reduce((a,b)->a+b).get());
                        }

                        // 设置品牌和分类参数
                        if (brandEntity != null) {
                            goods.setBrandId(brandEntity.getId());
                            goods.setBrandName(brandEntity.getName());
                            goods.setLogo(brandEntity.getLogo());
                        }
                        if (categoryEntity != null) {
                            goods.setCategoryId(categoryEntity.getId());
                            goods.setCategoryName(categoryEntity.getName());
                        }


                        // 设置检索类型的规格参数
                        List<SearchAttrValue> searchAttrValues = new ArrayList<>();
                        // 销售类型的检索属性
                        ResponseVo<List<SkuAttrValueEntity>> skuSearchAttrValueResponseVo = this.pmsClient.querySearchAttrValuesBycidAndSkuId(skuEntity.getCategoryId(), skuEntity.getId());
                        List<SkuAttrValueEntity> skuAttrValueEntities = skuSearchAttrValueResponseVo.getData();
                        if (!CollectionUtils.isEmpty(skuAttrValueEntities)){
                            searchAttrValues.addAll(skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                                SearchAttrValue searchAttrValue = new SearchAttrValue();
                                BeanUtils.copyProperties(skuAttrValueEntity, searchAttrValue);
                                return searchAttrValue;
                            }).collect(Collectors.toList()));
                        }
                        // 基本类型的检索属性
                        ResponseVo<List<SpuAttrValueEntity>> spuSearchAttrValueResponseVo = this.pmsClient.querySearchAttrValuesBycidAndSpuId(skuEntity.getCategoryId(), spuEntity.getId());
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

                    this.repository.saveAll(goodsList);
                }
            });


           pageSize = spuEntities.size();
           pageNum++;
       }while (pageSize==100);

    }

}
