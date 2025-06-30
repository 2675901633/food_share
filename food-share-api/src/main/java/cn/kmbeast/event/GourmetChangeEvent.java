package cn.kmbeast.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 美食变更事件
 */
@Getter
public class GourmetChangeEvent extends ApplicationEvent {
    private static final long serialVersionUID = 1L;

    /**
     * 美食ID
     */
    private final Integer gourmetId;

    /**
     * 操作类型
     */
    private final OperationType operationType;

    /**
     * 构造函数
     * 
     * @param source        事件源
     * @param gourmetId     美食ID
     * @param operationType 操作类型
     */
    public GourmetChangeEvent(Object source, Integer gourmetId, OperationType operationType) {
        super(source);
        this.gourmetId = gourmetId;
        this.operationType = operationType;
    }
}
