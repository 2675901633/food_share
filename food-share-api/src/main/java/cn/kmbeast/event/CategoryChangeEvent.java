package cn.kmbeast.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 分类变更事件
 */
@Getter
public class CategoryChangeEvent extends ApplicationEvent {
    private static final long serialVersionUID = 1L;

    /**
     * 分类ID
     */
    private final Integer categoryId;

    /**
     * 操作类型
     */
    private final OperationType operationType;

    /**
     * 构造函数
     * 
     * @param source        事件源
     * @param categoryId    分类ID
     * @param operationType 操作类型
     */
    public CategoryChangeEvent(Object source, Integer categoryId, OperationType operationType) {
        super(source);
        this.categoryId = categoryId;
        this.operationType = operationType;
    }
}