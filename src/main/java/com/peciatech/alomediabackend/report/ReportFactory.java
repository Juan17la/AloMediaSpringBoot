package com.peciatech.alomediabackend.report;

import com.peciatech.alomediabackend.report.dto.response.ReportData;

public abstract class ReportFactory {

    public abstract String getFormat();

    public abstract Object produce(ReportData data);

    public Object generateReport(ReportData data) {
        return produce(data);
    }
}
