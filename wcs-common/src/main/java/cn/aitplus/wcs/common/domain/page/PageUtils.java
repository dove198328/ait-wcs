package cn.aitplus.wcs.common.domain.page;

/**
 * 分页参数判断工具。
 */
public final class PageUtils {

    private PageUtils() {
    }

    public static boolean isPageQuery(Integer pageNum, Integer pageSize) {
        return pageNum != null && pageSize != null;
    }

    public static boolean hasOnlyOnePageParam(Integer pageNum, Integer pageSize) {
        return (pageNum == null) ^ (pageSize == null);
    }
}
