package com.example.agileagent.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExcelToJsonConverter {
    public static void convert(File excelFile, File jsonOutputFile) throws Exception {
        List<Map<String, String>> records = new ArrayList<>();
        try (InputStream is = new FileInputStream(excelFile);
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                Map<String, String> record = new LinkedHashMap<>();
                for (int j = 0; j < header.getLastCellNum(); j++) {
                    String key = header.getCell(j).getStringCellValue();
                    Cell cell = row.getCell(j);
                    String value = (cell != null) ? cell.toString() : "";
                    record.put(key, value);
                }
                records.add(record);
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(jsonOutputFile, records);
    }

    public static void main(String[] args) throws Exception {
        File excel = new File("src/main/resources/mock-data/Adjusted_Jira_Hygiene_Sample.xlsx");
        File json = new File("src/main/resources/mock-data/mock-agile-data.json");
        convert(excel, json);
        System.out.println("✅ Conversion complete: " + json.getAbsolutePath());
    }

}
