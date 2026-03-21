package com.peciatech.alomediabackend.report;

import com.peciatech.alomediabackend.report.dto.response.ReportData;

public class CsvReportFactory extends ReportFactory {

    @Override
    public String getFormat() {
        return "CSV";
    }

    @Override
    public Object produce(ReportData data) {
        String header = "totalUsers,totalProjects,totalProjectsCreated,totalProjectsEdited,totalProjectsExported,totalProjectsShared,generatedAt";
        String row = data.getTotalUsers() + "," +
                data.getTotalProjects() + "," +
                data.getTotalProjectsCreated() + "," +
                data.getTotalProjectsEdited() + "," +
                data.getTotalProjectsExported() + "," +
                data.getTotalProjectsShared() + "," +
                data.getGeneratedAt();
        return header + "\n" + row;
    }
}
