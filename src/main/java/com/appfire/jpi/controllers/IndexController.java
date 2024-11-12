package com.appfire.jpi.controllers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appfire.jpi.services.JiraExportService;
import static com.appfire.jpi.services.JiraExportService.FileType;

@RestController
public class IndexController {
    @Autowired
    private JiraExportService exportService;

    @RequestMapping("/export")
    public void exportJson() throws IOException, InterruptedException, ExecutionException {
        export(FileType.JSON);
    }

    @RequestMapping("/export/xml")
    public void exportXml() throws IOException, InterruptedException, ExecutionException {
        export(FileType.XML);
    }

    private void export(FileType fileType) throws IOException, InterruptedException, ExecutionException {
        var fileName = fileType == FileType.XML ? "output/issues.xml" : "output/issues.json";
        final File file = new File(fileName);
        final FileOutputStream out = new FileOutputStream(file);
        exportService.export(JiraExportService.FileType.XML, out);
        out.close();
    }
}
