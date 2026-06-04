package com.crd.rebalancing.models;

/**
 * Represents a single security in the portfolio with its rebalancing attributes.
 */
public class Security {

    private final String name;
    private final double targetPct;
    private final double currentPct;
    private final double unitPrice;

    public Security(String name, double targetPct, double currentPct, double unitPrice) {
        this.name       = name;
        this.targetPct  = targetPct;
        this.currentPct = currentPct;
        this.unitPrice  = unitPrice;
    }

    public String getName()       { return name;       }
    public double getTargetPct()  { return targetPct;  }
    public double getCurrentPct() { return currentPct; }
    public double getUnitPrice()  { return unitPrice;  }

    /**
     * variance = current% - target%
     *  negative → need to BUY
     *  positive → need to SELL
     *  zero     → HOLD
     */
    public double getVariance() {
        return currentPct - targetPct;
    }

    @Override
    public String toString() {
        return String.format("Security{name='%s', target=%.1f%%, current=%.1f%%, price=%.2f}",
                name, targetPct, currentPct, unitPrice);
    }
}
