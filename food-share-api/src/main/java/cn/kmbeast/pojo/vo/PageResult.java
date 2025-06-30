package cn.kmbeast.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页结果封装
 *
 * @param <T> 数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    /**
     * 数据列表
     */
    private List<T> data;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码
     */
    private Integer current;

    /**
     * 每页大小
     */
    private Integer size;

    /**
     * 创建分页结果
     * 
     * @param data    数据列表
     * @param total   总记录数
     * @param current 当前页码
     * @param size    每页大小
     * @param <T>     数据类型
     * @return 分页结果
     */
    public static <T> PageResult<T> of(List<T> data, Long total, Integer current, Integer size) {
        return new PageResult<>(data, total, current, size);
    }
}