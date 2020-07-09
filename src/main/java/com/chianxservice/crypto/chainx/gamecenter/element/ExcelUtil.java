package com.chianxservice.crypto.chainx.gamecenter.element;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExcelUtil {

	private Logger logger = LoggerFactory.getLogger(ExcelUtil.class);


	private String sepa = java.io.File.separator;
	
	private Workbook wb;
	private Sheet sheet;
	private Row row;

	/**
	 * 读取文件
	 * 
	 * @param filepath
	 * @return
	 */
	public List<List<Object>> getPurchaseInfo(String filepath) {
		List<List<Object>> list = null;
		if (filepath == null) {
			return null;
		}
		String ext = filepath.substring(filepath.lastIndexOf("."));
		try {
			InputStream is = new FileInputStream(filepath);
			if (".xlsx".equals(ext)) {
				wb = new XSSFWorkbook(is);
			} else {
				wb = null;
			}
			list = readExcelContent();
		} catch (FileNotFoundException e) {
			logger.error("FileNotFoundException", e);
		} catch (IOException e) {
			logger.error("IOException", e);
		} catch (Exception e) {
			logger.error("Exception", e);
		}
		return list;
	}

	private List<List<Object>> readExcelContent() throws Exception {

		if (wb == null) {
			throw new Exception("Workbook对象为空！");
		}
		List<List<Object>> list = new ArrayList<List<Object>>();
		List<Object> rows = null;
		sheet = wb.getSheetAt(0);
		// 得到总行数
		int rowNum = sheet.getLastRowNum();
		System.out.println("总行数 : "+rowNum);
		int columnNum = sheet.getRow(0).getPhysicalNumberOfCells();
		System.out.println("总列数 : "+columnNum);
		row = sheet.getRow(0);

		for (int i = 0; i <= rowNum; i++) {
			row = sheet.getRow(i);
			rows = new ArrayList<>();
			list.add(rows);
			for (int k = 0; k <= columnNum; k++) {
				if (row.getCell(k) != null) {
					rows.add(row.getCell(k));
				}
			}
		}
		return list;
	}

	/**
	 * 创建工作簿
	 * 
	 * @param sfworkbook
	 * @param headerstr
	 * @return
	 */
	public SXSSFWorkbook createWorkbook(XSSFWorkbook sfworkbook) {
		SXSSFWorkbook sxssfWorkbook = new SXSSFWorkbook(sfworkbook, 1000);
//		SXSSFSheet xfsheet = null;
//		xfsheet = (SXSSFSheet) sxssfWorkbook.createSheet("sheet");
		return sxssfWorkbook;
	}

	public static interface WriteExcel<T> {
		void writeHead(XSSFRow contentxfrow);

		void write(XSSFRow contentxfrow, T t);
	}

	/**
	 * 创建工作簿头
	 * 
	 * @param sfworkbook
	 * @param headerstr
	 * @return
	 */
	public void createHead(XSSFWorkbook sfworkbook, SXSSFWorkbook sxssfWorkbook, WriteExcel writeexcel) {
		XSSFSheet xfsheet = null;
		xfsheet = sfworkbook.getSheet("sheet");
		XSSFRow xfrow = xfsheet.createRow(0);
		writeexcel.writeHead(xfrow);
	}

	/**
	 * 向文件写入内容
	 * 
	 * @param sfworkbook
	 * @param sxssfWorkbook
	 * @param list
	 * @param rowNum
	 */

	public <T> void writeXlsFile(XSSFWorkbook sfworkbook, SXSSFWorkbook sxssfWorkbook, List<T> list,
			WriteExcel writeexcel) {
		XSSFSheet xfsheet = null;
		xfsheet = sfworkbook.getSheet("sheet");
		for (int i = 0; i < list.size(); i++) {
			XSSFRow contentxfrow = xfsheet.createRow(i + 1);
			writeexcel.write(contentxfrow, list.get(i));
		}
	}
	

	public static void main(String[] args) {
		String path = "/Users/admin/Desktop/test/test.xlsx";
		ExcelUtil excelUtil = new ExcelUtil();
		List<List<Object>> purchaseInfo = excelUtil.getPurchaseInfo(path);
		for (List<Object> list : purchaseInfo) {
			for (Object object : list) {
				String string = object.toString();
				// System.out.println(string.indexOf(".0"));
				if (string.lastIndexOf(".0") != -1) {
					string = string.substring(0, string.lastIndexOf(".0"));
				}
				System.out.println(string);
			}
		}
	}
}
