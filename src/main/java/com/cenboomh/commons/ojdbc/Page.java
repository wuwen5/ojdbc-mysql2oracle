package com.cenboomh.commons.ojdbc;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author wuwen
 */
@Getter
@Setter
@Accessors(chain = true)
public class Page {

    private int startRowIndex;

    private Integer startRowValue;

    private int endRowIndex;

    private Integer endRowValue;
}
