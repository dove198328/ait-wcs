package cn.aitplus.wcs.common.domain.page;

import cn.hutool.core.util.ObjectUtil;

import java.io.Serializable;

/**
 * 分页查询实体类
 */
public class PageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分页大小
     */
    private Integer pageSize;

    /**
     * 当前页数
     */
    private Integer pageNum;

    /**rc
     * 当前记录起始索引 默认值
     */
    public static final int DEFAULT_PAGE_NUM = 1;

    /**
     * 每页显示记录数 默认值 默认查全部
     */
    public static final int DEFAULT_PAGE_SIZE = Integer.MAX_VALUE;

    /**
     * 当前记录起始索引关键字
     */
    public static final String PAGE_NUM = "pageNum";

    /**
     * 每页显示记录数关键字
     */
    public static final String PAGE_SIZE = "pageSize";

    public static class PageParam<T> implements Serializable {
        private static final long serialVersionUID = 1L;
        private long pageNum;
        private long pageSize;
        private boolean optimizeJoinOfCountSql;

        public PageParam(long pageNum, long pageSize) {
            this.pageNum = pageNum;
            this.pageSize = pageSize;
        }

        public long getPageNum() {
            return pageNum;
        }

        public void setPageNum(long pageNum) {
            this.pageNum = pageNum;
        }

        public long getPageSize() {
            return pageSize;
        }

        public void setPageSize(long pageSize) {
            this.pageSize = pageSize;
        }

        public boolean isOptimizeJoinOfCountSql() {
            return optimizeJoinOfCountSql;
        }

        public void setOptimizeJoinOfCountSql(boolean optimizeJoinOfCountSql) {
            this.optimizeJoinOfCountSql = optimizeJoinOfCountSql;
        }
    }

    /**
     * 构建分页对象
     * @return  page
     * @param <T> 泛型
     */
    public <T> PageParam<T> build() {
        Integer num = ObjectUtil.defaultIfNull(getPageNum(), DEFAULT_PAGE_NUM);
        Integer size = ObjectUtil.defaultIfNull(getPageSize(), DEFAULT_PAGE_SIZE);
        if (num <= 0) {
            num = DEFAULT_PAGE_NUM;
        }
        PageParam<T> page = new PageParam<>(num, size);
        page.setOptimizeJoinOfCountSql(false);
        return page;
    }

    /**
     *  从请求中获取分页参数
     * @return  page
     * @param <T> 泛型
     */
    public static  <T> PageParam<T> buildFromRequest() {
        PageParam<T> page = new PageParam<>(DEFAULT_PAGE_NUM, DEFAULT_PAGE_SIZE);
        page.setOptimizeJoinOfCountSql(false);
        return page;
    }

    /**
     *      通过参数判断是否是分页查询
     * @return boolean
     */
    public static boolean isPageQuery() {
        return false;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }
}
