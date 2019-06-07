package com.nadomescanja_parser;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.github.barteksc.pdfviewer.PDFView;

import java.io.File;

public class PDFVIEW extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdfview);
        File pdf = (File)getIntent().getSerializableExtra("pdf");
        PDFView pdfView = (PDFView) findViewById(R.id.pdfView);
        pdfView.fromFile(pdf).load();
    }
}
