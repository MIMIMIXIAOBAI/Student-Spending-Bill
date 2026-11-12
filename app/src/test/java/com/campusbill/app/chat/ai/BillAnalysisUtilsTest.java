package com.campusbill.app.chat.ai;

import static org.junit.Assert.assertEquals;
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

    @Test
    public void budgetState_四态判定() {
        // 新装：未设预算、无账单 → 未设置，且绝不能渲染成超支
        assertEquals(BillAnalysisUtils.BUDGET_UNSET, BillAnalysisUtils.budgetState(0.0, 0.0));
        // 设了预算但未导入账单：仍应显示剩余 1500，而不是“请先导入账单”
        assertEquals(BillAnalysisUtils.BUDGET_OK, BillAnalysisUtils.budgetState(1500.0, 0.0));
        assertEquals(1500.0, BillAnalysisUtils.budgetRemaining(1500.0, 0.0), 0.001);
        // 演示机：预算 1500、本月已花 210 → 剩余 1290
        assertEquals(BillAnalysisUtils.BUDGET_OK, BillAnalysisUtils.budgetState(1500.0, 210.0));
        assertEquals(1290.0, BillAnalysisUtils.budgetRemaining(1500.0, 210.0), 0.001);
        // 真实超支不能被中性分支吞掉：预算 2000、已花 2100 → 超支
        assertEquals(BillAnalysisUtils.BUDGET_OVER, BillAnalysisUtils.budgetState(2000.0, 2100.0));
        assertTrue(BillAnalysisUtils.budgetRemaining(2000.0, 2100.0) < 0);
    }
}
