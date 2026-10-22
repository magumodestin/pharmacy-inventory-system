package com.healthfirst.pims.model;

import java.math.BigDecimal;

/**
 * Represents one line in the POS cart / a persisted sale_items row.
 */
public class SaleItem {
    private int saleItemId;
    private int saleId;
    private int medicineId;
    private String medicineName; // convenience for display
    private int quantitySold;
    private BigDecimal priceAtSale;

    public SaleItem() {}

    public SaleItem(int medicineId, String medicineName, int quantitySold, BigDecimal priceAtSale) {
        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.quantitySold = quantitySold;
        this.priceAtSale = priceAtSale;
    }

    public BigDecimal getLineTotal() {
        return priceAtSale.multiply(BigDecimal.valueOf(quantitySold));
    }

    public int getSaleItemId() { return saleItemId; }
    public void setSaleItemId(int saleItemId) { this.saleItemId = saleItemId; }

    public int getSaleId() { return saleId; }
    public void setSaleId(int saleId) { this.saleId = saleId; }

    public int getMedicineId() { return medicineId; }
    public void setMedicineId(int medicineId) { this.medicineId = medicineId; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public int getQuantitySold() { return quantitySold; }
    public void setQuantitySold(int quantitySold) { this.quantitySold = quantitySold; }

    public BigDecimal getPriceAtSale() { return priceAtSale; }
    public void setPriceAtSale(BigDecimal priceAtSale) { this.priceAtSale = priceAtSale; }
}
