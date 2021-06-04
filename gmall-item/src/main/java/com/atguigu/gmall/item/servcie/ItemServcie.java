package com.atguigu.gmall.item.servcie;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.item.feign.GmallPmsClient;
import com.atguigu.gmall.item.feign.GmallSmsClient;
import com.atguigu.gmall.item.feign.GmallWmsClient;
import com.atguigu.gmall.item.vo.ItemVo;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class ItemServcie {
    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private TemplateEngine templateEngine;

    public ItemVo loadItem(Long skuId) {
        ItemVo itemVo = new ItemVo();

        // 1.根据skuId查询sku
        CompletableFuture<SkuEntity> skuFuture = CompletableFuture.supplyAsync(() -> {
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(skuId);
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity == null) {
                throw new RuntimeException("该商品不存在！");
            }
            itemVo.setSkuId(skuEntity.getId());
            itemVo.setTitle(skuEntity.getTitle());
            itemVo.setSubTitle(skuEntity.getSubtitle());
            itemVo.setPrice(skuEntity.getPrice());
            itemVo.setWeight(skuEntity.getWeight());
            itemVo.setDefaultImages(skuEntity.getDefaultImage());
            return skuEntity;
        }, threadPoolExecutor);

        //2.根据三级分类的id查询一二三级分类
        CompletableFuture<Void> catesFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<List<CategoryEntity>> catesResponseVo = this.pmsClient.queryLvl123CatesByCid3(skuEntity.getCategoryId());
            List<CategoryEntity> categoryEntities = catesResponseVo.getData();
            itemVo.setCategories(categoryEntities);
        }, threadPoolExecutor);

        // 3.根据品牌id查询品牌  V
        CompletableFuture<Void> brandFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(skuEntity.getBrandId());
            BrandEntity brandEntity = brandEntityResponseVo.getData();
            if (brandEntity != null) {
                itemVo.setBrandId(brandEntity.getId());
                itemVo.setBrandName(brandEntity.getName());
            }
        }, threadPoolExecutor);

        // 4.根据spuId查询spu V
        CompletableFuture<Void> spuFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsClient.querySpuById(skuEntity.getSpuId());
            SpuEntity spuEntity = spuEntityResponseVo.getData();
            if (spuEntity != null) {
                itemVo.setSpuId(spuEntity.getId());
                itemVo.setSpuName(spuEntity.getName());
            }
        }, threadPoolExecutor);

        // 5.根据skuId查询营销信息（三类） V
        CompletableFuture<Void> salesFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<ItemSaleVo>> salesResponseVo = this.smsClient.querySaleListBySkuId(skuId);
            List<ItemSaleVo> itemSaleVos = salesResponseVo.getData();
            itemVo.setSales(itemSaleVos);
        }, threadPoolExecutor);

        // 6.根据skuId查询sku的图片列表 V
        CompletableFuture<Void> imagesFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<SkuImagesEntity>> imagesResponseVo = this.pmsClient.queryImagesBySkuId(skuId);
            List<SkuImagesEntity> skuImagesEntities = imagesResponseVo.getData();
            itemVo.setImages(skuImagesEntities);
        }, threadPoolExecutor);

        // 7.根据skuId查询库存列表 V
        CompletableFuture<Void> storeFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<WareSkuEntity>> wareResponseVo = this.wmsClient.queryWareSkuList(skuId);
            List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                itemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }
        }, threadPoolExecutor);

        // 8.根据spuId查询spu下所有sku的销售属性 V
        CompletableFuture<Void> saleAttrsFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<List<SaleAttrValueVo>> saleAttrsResponseVo = this.pmsClient.querySaleAttrValuesBySpuId(skuEntity.getSpuId());
            List<SaleAttrValueVo> saleAttrValueVos = saleAttrsResponseVo.getData();
            itemVo.setSaleAttrs(saleAttrValueVos);
        }, threadPoolExecutor);

        // 9.根据skuId查询当前sku的销售属性 V  {3: 白天白, 4: 8G, 6: 128G}
        CompletableFuture<Void> saleAttrFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<SkuAttrValueEntity>> saleAttrResponseVo = this.pmsClient.querySaleAttrValuesBySkuId(skuId);
            List<SkuAttrValueEntity> skuAttrValueEntities = saleAttrResponseVo.getData();
            if (!CollectionUtils.isEmpty(skuAttrValueEntities)) {
                itemVo.setSaleAttr(skuAttrValueEntities.stream().collect(Collectors.toMap(SkuAttrValueEntity::getAttrId, SkuAttrValueEntity::getAttrValue)));
            }
        }, threadPoolExecutor);

        // 10.根据spuId查询spu下所有销售属性组合与skuId的映射关系 V
        CompletableFuture<Void> mappingFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<String> mappingResponseVo = this.pmsClient.querySaleAttrValuesMappingSkuIdBySpuId(skuEntity.getSpuId());
            String json = mappingResponseVo.getData();
            itemVo.setSkuJsons(json);
        }, threadPoolExecutor);

        // 11.根据spuId查询描述信息 V
        CompletableFuture<Void> descFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<SpuDescEntity> spuDescEntityResponseVo = this.pmsClient.querySpuDescById(skuEntity.getSpuId());
            SpuDescEntity spuDescEntity = spuDescEntityResponseVo.getData();
            if (spuDescEntity != null) {
                itemVo.setSpuImages(Arrays.asList(StringUtils.split(spuDescEntity.getDecript(), ",")));
            }
        }, threadPoolExecutor);

        // 12.根据分类id，spuId，skuId查询规格参数分组及组下的规格参数和值
        CompletableFuture<Void> groupFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<List<ItemGroupVo>> groupResponseVo = this.pmsClient.queryGroupWithAttrValuesByCidAndSpuIdAndSkuId(skuEntity.getCategoryId(), skuEntity.getSpuId(), skuId);
            List<ItemGroupVo> groupVos = groupResponseVo.getData();
            itemVo.setGroups(groupVos);
        }, threadPoolExecutor);

        CompletableFuture.allOf(catesFuture, brandFuture, spuFuture, salesFuture, imagesFuture,
                storeFuture, saleAttrsFuture, saleAttrFuture, mappingFuture, descFuture, groupFuture).join();

        threadPoolExecutor.execute(() -> {
            this.generateHtml(itemVo);
        });

        return itemVo;
    }

    private void generateHtml(ItemVo itemVo) {
        try ( PrintWriter printWriter = new PrintWriter(new File("D:\\静态页面\\html\\" + itemVo.getSkuId() + ".html"))){
            Context context = new Context();
            context.setVariable("itemVo",itemVo);
            templateEngine.process("item",context,printWriter);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
