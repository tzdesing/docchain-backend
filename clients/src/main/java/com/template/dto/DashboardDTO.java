package com.template.dto;

import java.math.BigDecimal;

public class DashboardDTO {
    private int totalContracts;
    private int totalContractUpdates;
    private BigDecimal totalContractValue;
    private String barChartTotalContractsByMonth;
    private String barChartTotalUpdateByMonth;

    public DashboardDTO() {
        this.totalContracts = 0;
        this.totalContractUpdates = 0;
        this.totalContractValue = new BigDecimal("0.00");
        this.barChartTotalContractsByMonth = "";
        this.barChartTotalUpdateByMonth = "";
    }

    public DashboardDTO(int totalContracts, int totalContractUpdates, BigDecimal totalContractValue, String barChartTotalContractsByMonth, String barChartTotalUpdateByMonth) {
        this.totalContracts = totalContracts;
        this.totalContractUpdates = totalContractUpdates;
        this.totalContractValue = totalContractValue;
        this.barChartTotalContractsByMonth = barChartTotalContractsByMonth;
        this.barChartTotalUpdateByMonth = barChartTotalUpdateByMonth;
    }

    public int getTotalContracts() {
        return totalContracts;
    }

    public void setTotalContracts(int totalContracts) {
        this.totalContracts = totalContracts;
    }

    public int getTotalContractUpdates() {
        return totalContractUpdates;
    }

    public void setTotalContractUpdates(int totalContractUpdates) {
        this.totalContractUpdates = totalContractUpdates;
    }

    public BigDecimal getTotalContractValue() {
        return totalContractValue;
    }

    public void setTotalContractValue(BigDecimal totalContractValue) {
        this.totalContractValue = totalContractValue;
    }

    public String getBarChartTotalContractsByMonth() {
        return barChartTotalContractsByMonth;
    }

    public void setBarChartTotalContractsByMonth(String barChartTotalContractsByMonth) {
        this.barChartTotalContractsByMonth = barChartTotalContractsByMonth;
    }

    public String getBarChartTotalUpdateByMonth() {
        return barChartTotalUpdateByMonth;
    }

    public void setBarChartTotalUpdateByMonth(String barChartTotalUpdateByMonth) {
        this.barChartTotalUpdateByMonth = barChartTotalUpdateByMonth;
    }
}
