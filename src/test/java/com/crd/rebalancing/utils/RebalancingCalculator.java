package com.crd.rebalancing.utils;

import com.crd.rebalancing.models.Security;

/**
 * Pure-Java mirror of the JavaScript calculation logic in index.html.
 * Used to derive expected values in test assertions.
 *
 * Formula:
 *   variance ($)  = |currentPct - targetPct| / 100 * totalAssets
 *   shares        = floor(variance$ / unitPrice)
 *   action        = BUY  if currentPct < targetPct
 *                   SELL if currentPct > targetPct
 *                   HOLD if currentPct == targetPct
 */
public class RebalancingCalculator {

    public enum Action { BUY, SELL, HOLD }

    public static int calculateShares(Security sec, double totalAssets) {
        double variancePct   = Math.abs(sec.getVariance());
        double dollarAmount  = variancePct / 100.0 * totalAssets;
        return (int) Math.floor(dollarAmount / sec.getUnitPrice());
    }

    public static Action calculateAction(Security sec) {
        double variance = sec.getVariance(); // current - target
        if (variance < 0) return Action.BUY;
        if (variance > 0) return Action.SELL;
        return Action.HOLD;
    }

    public static double calculateDollarAmount(Security sec, double totalAssets) {
        double variancePct = Math.abs(sec.getVariance());
        return variancePct / 100.0 * totalAssets;
    }
}
