package org.example;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import dnl.utils.text.table.TextTable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class TextOutput {


    public static void jsonOutput(String jsonName,String jsonString){
        System.out.println();
        System.out.println(jsonName);
        System.out.println(jsonString);
    }
}
