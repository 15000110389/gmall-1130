package com.atguigu.gmall.pms.api;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface GmallPmsApi {
//    @GetMapping("pms/attrgroup/attr/value/{cid}")
//    public ResponseVo<List<ItemGroupVo>> queryGroupWithAttrValuesByCidAndSpuIdAndSkuId(
//            @PathVariable("cid")Long cid,
//            @RequestParam("spuId")Long spuId,
//            @RequestParam("skuId")Long skuId);
//    @GetMapping("pms/spudesc/{spuId}")
//    @ApiOperation("详情查询")
//    public ResponseVo<SpuDescEntity> querySpuDescById(@PathVariable("spuId") Long spuId);
//    @GetMapping("pms/skuattrvalue/sku/{skuId}")
//    public ResponseVo<List<SkuAttrValueEntity>> querySaleAttrValuesBySkuId(@PathVariable("skuId")Long skuId);
//    @GetMapping("pms/skuattrvalue/spu/{spuId}")
//    public ResponseVo<List<SaleAttrValueVo>> querySaleAttrValuesByspuId(@PathVariable("spuId")Long spuId);
//    @GetMapping("pms/skuimages/sku/{skuId}")
//    public ResponseVo<List<SkuImagesEntity>> querySkuImagesBySkuId(@PathVariable("skuId")Long skuId);
//    @GetMapping("pms/category/all/{cid}")
//    public ResponseVo<List<CategoryEntity>> queryCatesByCid3(@PathVariable("cid") Long cid);
//    @GetMapping("pms/sku/{id}")
//    @ApiOperation("详情查询")
//    public ResponseVo<SkuEntity> querySkuById(@PathVariable("id") Long id);
//    @GetMapping("pms/spu/{id}")
//    @ApiOperation("详情查询")
//    public ResponseVo<SpuEntity> querySpuById(@PathVariable("id") Long id);
//    @PostMapping("pms/spu/page/json")
//    @ApiOperation("分页查询")
//    public ResponseVo<List<SpuEntity>> querySpuByPageJson(@RequestBody PageParamVo paramVo);
//
//    @GetMapping("pms/sku/spu/{spuId}")
//    public ResponseVo<List<SkuEntity>> querySkuList(@PathVariable("spuId")Long spuId);
//
//    @GetMapping("pms/brand/{id}")
//    @ApiOperation("详情查询")
//    public ResponseVo<BrandEntity> queryBrandById(@PathVariable("id") Long id);
//
//    @GetMapping("pms/category/{id}")
//    @ApiOperation("详情查询")
//    public ResponseVo<CategoryEntity> queryCategoryById(@PathVariable("id") Long id);
//
//    @GetMapping("pms/spuattrvalue/search/{cid}")
//    public ResponseVo<List<SpuAttrValueEntity>> querySearchAttrValuesBycidAndSpuId(
//            @PathVariable("cid")Long cid, @RequestParam("spuId")Long spuId
//    );
//
//    @GetMapping("pms/skuattrvalue/search/{cid}")
//    public ResponseVo<List<SkuAttrValueEntity>> querySearchAttrValuesBycidAndSkuId(
//            @PathVariable("cid")Long cid, @RequestParam("skuId")Long skuId
//    );
@PostMapping("pms/spu/page/json")
public ResponseVo<List<SpuEntity>> querySpuByPageJson(@RequestBody PageParamVo paramVo);

    @GetMapping("pms/spu/{id}")
    public ResponseVo<SpuEntity> querySpuById(@PathVariable("id") Long id);

    @GetMapping("pms/sku/spu/{spuId}")
    public ResponseVo<List<SkuEntity>> querySkuList(@PathVariable("spuId")Long spuId);

    @GetMapping("pms/sku/{id}")
    @ApiOperation("详情查询")
    public ResponseVo<SkuEntity> querySkuById(@PathVariable("id") Long id);

    @GetMapping("pms/brand/{id}")
    public ResponseVo<BrandEntity> queryBrandById(@PathVariable("id") Long id);

    @GetMapping("pms/category/{id}")
    public ResponseVo<CategoryEntity> queryCategoryById(@PathVariable("id") Long id);

    @GetMapping("pms/category/all/{cid}")
    public ResponseVo<List<CategoryEntity>> queryLvl123CatesByCid3(@PathVariable("cid")Long cid);

    @GetMapping("pms/category/parent/{parentId}")
    public ResponseVo<List<CategoryEntity>> queryCategoriesByPid(@PathVariable("parentId")Long pid);

    @GetMapping("pms/category/subs/{pid}")
    public ResponseVo<List<CategoryEntity>> queryLvl2WithSubByPid(@PathVariable("pid")Long pid);

    @GetMapping("pms/skuattrvalue/search/{cid}")
    public ResponseVo<List<SkuAttrValueEntity>> querySearchAttrValuesByCidAndSkuId(
            @PathVariable("cid")Long cid,
            @RequestParam("skuId")Long skuId
    );

    @GetMapping("pms/skuattrvalue/spu/{spuId}")
    public ResponseVo<List<SaleAttrValueVo>> querySaleAttrValuesBySpuId(@PathVariable("spuId")Long spuId);

    @GetMapping("pms/skuattrvalue/sku/{skuId}")
    public ResponseVo<List<SkuAttrValueEntity>> querySaleAttrValuesBySkuId(@PathVariable("skuId")Long skuId);

    @GetMapping("pms/skuattrvalue/mapping/{spuId}")
    public ResponseVo<String> querySaleAttrValuesMappingSkuIdBySpuId(@PathVariable("spuId")Long spuId);

    @GetMapping("pms/spuattrvalue/search/{cid}")
    public ResponseVo<List<SpuAttrValueEntity>> querySearchAttrValuesByCidAndSpuId(
            @PathVariable("cid")Long cid,
            @RequestParam("spuId")Long spuId
    );

    @GetMapping("pms/skuimages/sku/{skuId}")
    public ResponseVo<List<SkuImagesEntity>> queryImagesBySkuId(@PathVariable("skuId")Long skuId);

    @GetMapping("pms/spudesc/{spuId}")
    public ResponseVo<SpuDescEntity> querySpuDescById(@PathVariable("spuId") Long spuId);

    @GetMapping("pms/attrgroup/attr/value/{cid}")
    public ResponseVo<List<ItemGroupVo>> queryGroupWithAttrValuesByCidAndSpuIdAndSkuId(
            @PathVariable("cid")Long cid,
            @RequestParam("spuId")Long spuId,
            @RequestParam("skuId")Long skuId
    );
}
