package com.peciatech.alomediabackend.report;

import com.peciatech.alomediabackend.report.dto.response.ReportData;

public class JsonReportFactory extends ReportFactory {

    @Override
    public String getFormat() {
        return "JSON";
    }

    @Override
    public Object produce(ReportData data) {
        return data;
    }
}
