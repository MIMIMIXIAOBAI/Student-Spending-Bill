package com.campusbill.app.chat.ai;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * 预算状态判定的单元测试（纯 Java，不依赖设备）。
 */
public class BillAnalysisUtilsTest {

    @Test
    public void hasBudget_预算小于等于0视为未设置() {
        assertFalse(BillAnalysisUtils.hasBudget(0.0));
        assertFalse(BillAnalysisUtils.hasBudget(-1.0));
        assertTrue(BillAnalysisUtils.hasBudget(1500.0));
    }
}
