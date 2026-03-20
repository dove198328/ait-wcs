package cn.aitplus.wcs.core.spi;

import java.util.List;

public interface ChainRunner<C, N, R> {

    R run(C context, List<N> chainNodes);
}
