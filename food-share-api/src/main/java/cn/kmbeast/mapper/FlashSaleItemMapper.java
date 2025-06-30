package cn.kmbeast.mapper;

import cn.kmbeast.pojo.dto.query.extend.FlashSaleItemQueryDto;
import cn.kmbeast.pojo.entity.FlashSaleItem;
import cn.kmbeast.pojo.vo.FlashSaleItemVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 秒杀商品Mapper接口
 */
@Mapper
public interface FlashSaleItemMapper extends BaseMapper<FlashSaleItem> {

    /**
     * 查询秒杀商品列表
     * 
     * @param queryDto 查询条件
     * @return 秒杀商品列表
     */
    List<FlashSaleItemVO> queryFlashSaleItems(FlashSaleItemQueryDto queryDto);

    /**
     * 根据ID查询秒杀商品
     * 
     * @param id 商品ID
     * @return 秒杀商品信息
     */
    FlashSaleItemVO getFlashSaleItemById(@Param("id") Integer id);

    /**
     * 查询正在进行中的秒杀商品
     * 
     * @return 进行中的秒杀商品列表
     */
    List<FlashSaleItemVO> queryOngoingFlashSaleItems();

    /**
     * 更新商品库存
     * 
     * @param itemId 商品ID
     * @param stock  更新后的库存
     * @return 更新行数
     */
    int updateStock(@Param("itemId") Integer itemId, @Param("stock") Integer stock);

    /**
     * 扣减库存
     * 
     * @param itemId 商品ID
     * @return 更新行数
     */
    int decreaseStock(@Param("itemId") Integer itemId);

    /**
     * 查询所有秒杀商品ID
     * 
     * @return 商品ID列表
     */
    List<Integer> getAllItemIds();
}