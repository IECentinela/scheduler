package com.centinela.scheduler.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.centinela.scheduler.dto.Counts;
import com.centinela.scheduler.dto.Data;
import com.centinela.scheduler.dto.RawLocation;
import com.centinela.scheduler.dto.User;
import com.google.gson.Gson;

public class FilesUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(FilesUtil.class);
	
	public static void downloadCsvFromFTP(String server, int port, String username, String password, String remoteFilePath, String localFilePath) {
		FTPClient ftpClient = new FTPClient();
		try {
			
			logger.info(server);
			logger.info(String.valueOf(port));
			logger.info(username);
			logger.info(password);
			ftpClient.connect(server, port);
			ftpClient.login(username, password);
			ftpClient.enterLocalPassiveMode();
			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

			File localFile = new File(localFilePath);
			logger.debug("Descargando archivo CSV desde el servidor FTP...");
			try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(localFile))) {
				ftpClient.retrieveFile(remoteFilePath, outputStream);
			}

			logger.debug("Archivo CSV descargado exitosamente.");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (ftpClient.isConnected()) {
					ftpClient.logout();
					ftpClient.disconnect();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static String convertCsvToExcel(String csvFilePath) {
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("Sheet1");

		try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
			String line;
			int rowNum = 0;
			while ((line = br.readLine()) != null) {
				Row row = sheet.createRow(rowNum++);
				line = reemplazarComasDentroDeComillas(line);
				String[] data = dividirCadena(line);
				int cellNum = 0;
				for (String value : data) {
					Cell cell = row.createCell(cellNum++);
					cell.setCellValue(value);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		String excelFilePath = csvFilePath.replace(".csv", "_converted.xlsx");
		try (FileOutputStream fileOut = new FileOutputStream(excelFilePath)) {
			workbook.write(fileOut);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return excelFilePath;
	}
	
	public static void uploadFileToFTP(String server, int port, String username, String password, String remoteFilePath,
			String localFilePath) {
		FTPClient ftpClient = new FTPClient();

		try {
			ftpClient.connect(server, port);
			ftpClient.login(username, password);
			ftpClient.enterLocalPassiveMode();
			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
			File localFile = new File(localFilePath);
			logger.debug("Subiendo archivo Excel al servidor FTP...");
			try (InputStream inputStream = Files.newInputStream(localFile.toPath())) {
				ftpClient.storeFile(remoteFilePath, inputStream);
			}catch(Exception e) {
				logger.error("Error al subir archivo a FTP...");
			}

			logger.debug("Archivo Excel subido exitosamente.");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (ftpClient.isConnected()) {
					ftpClient.logout();
					ftpClient.disconnect();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static String procesaExcelGraphtext(String ftpServer, String user, String password, String rutaExcel) throws IOException, NumberFormatException {
		
		FTPClient ftp = new FTPClient();
		List<Data> menciones = new ArrayList<Data>();
		Workbook workbook = null;
		try {
			ftp.connect(ftpServer);
			ftp.login(user, password);
			ftp.enterLocalPassiveMode();
			ftp.setFileType(FTP.BINARY_FILE_TYPE);

			try (InputStream inputStream = ftp.retrieveFileStream(rutaExcel)) {
				workbook = new XSSFWorkbook(inputStream);
			Sheet graphtext = workbook.getSheetAt(0);

			if (graphtext != null) {
				for (int i = 1; i <= graphtext.getLastRowNum(); i++) {
					//logger.debug("Fila: "+i);
					Row row = graphtext.getRow(i);
					Data mencion = new Data();
					User usuario = new User();
					Counts countsMencion = new Counts();
					Counts countsUser = new Counts();
					RawLocation rawLocationMencion = new RawLocation();

					for (int j = 0; j < row.getLastCellNum(); j++) {
						Cell cell = row.getCell(j);
						//logger.debug("Celda: "+j+" Valor celda: "+cell.toString());
						if (cell != null) {
							if (j == 0) {// Id de la MENCION
								mencion.set_id(cell.getStringCellValue());
							} else if (j == 1) {// Id de USUARIO
								usuario.setId(cell.getStringCellValue());
							} else if (j == 2) {// Nombre de USUARIO
								usuario.setName(cell.getStringCellValue());
							} else if (j == 3) {// Alias de USUARIO
								usuario.setAlias(cell.getStringCellValue());
							} else if (j == 4) {// Foto del USUARIO
								usuario.setPicture(cell.getStringCellValue());
							} else if (j == 5) {// Fecha createdAt del USUARIO

							} else if (j == 6) {// Bio del USUARIO
								usuario.setBio(cell.getStringCellValue().replaceAll("[\\[\\]]", ""));
							} else if (j == 7) {// Followers USUARIO
								try {
									usuario.setFollowers(Integer.valueOf(cell.getStringCellValue()));
								}catch(NumberFormatException e) {
									logger.error("Fila: "+i);
									logger.error("Error al convertir numero: "+e.getMessage());
									
								}
								
							} else if (j == 8) {// Following USUARIO
								usuario.setFollowing(Integer.valueOf(cell.getStringCellValue()));
							} else if (j == 9) {// Lists CONTADORES USUARIO
								countsUser.setLists(Integer.valueOf(cell.getStringCellValue()));
							} else if (j == 10) {// Tweets CONTADORES USUARIO

							} else if (j == 11) {// Verified del USUARIO
								usuario.setVerified(Boolean.parseBoolean(cell.getStringCellValue()));
							} else if (j == 12) {// Location del USUARIO
								usuario.setLocation(cell.getStringCellValue());
							} else if (j == 13) {// Lenguaje de la MENCION
								mencion.setLang(cell.getStringCellValue());
							} else if (j == 14) {// Tipo de la MENCION
								mencion.setType(cell.getStringCellValue());
							} else if (j == 15) {// Texto de la MENCION
								mencion.setText(cell.getStringCellValue());
							} else if (j == 16) {// Fecha de la MENCION
								mencion.setCreatedAt(cell.getStringCellValue());
							} else if (j == 17) {// mention_names<gx:list[category]>
								mencion.setMentions(cell.getStringCellValue());
							} else if (j == 18) {// retweets<gx:number> CONTADORES MENCION
								countsMencion.setRetweets(Integer.valueOf(cell.getStringCellValue()));
								mencion.setRetweets(Integer.valueOf(cell.getStringCellValue()));
							} else if (j == 19) {// favorites<gx:number> CONTADORES MENCION
								countsMencion.setFavorites(Integer.valueOf(cell.getStringCellValue()));
							} else if (j == 20) {// replies<gx:number> CONTADORES MENCION
								countsMencion.setReplies(Integer.valueOf(cell.getStringCellValue()));
							} else if (j == 21) {// links<gx:list[url]> MENCION
								mencion.setLinks(cell.getStringCellValue());
							} else if (j == 23) {// image_links<gx:list[url]> MENCION
								mencion.setImages(cell.getStringCellValue());
							} else if (j == 25) {// location<gx:text> MENCION
								rawLocationMencion.setLocationString(cell.getStringCellValue());
							}
						}
					}
					usuario.setCounts(countsUser);
					mencion.setUser(usuario);
					mencion.setCounts(countsMencion);
					menciones.add(mencion);
				}
			}
			workbook.close();
		} catch (IOException e) {
			logger.error("Error al procesar FTP: " + e.getMessage());
			e.printStackTrace();
		} catch (NumberFormatException  e) {
			logger.error("Error al procesar numero: " + e.getMessage());
			e.printStackTrace();
		}
		} catch (IOException e) {

			logger.error("Error al lerr FTP: " + e.getMessage());
			e.printStackTrace();
		}

		Gson gson = new Gson();
//		logger.debug(gson.toJson(menciones).toString().replace("\\\"", ""));
		String textValue = gson.toJson(menciones).toString().replace("\\\"", "");
		String updatedTextValue = textValue.replaceAll("\n\n", "");
		String updatedTextValue2 = updatedTextValue.replaceAll("\n", "");
		String updatedTextValue3 = updatedTextValue2.replaceAll("\n", "");
		String updatedTextValue4 = updatedTextValue3.replaceAll("\\u2013", "");
		String updatedTextValue5 = updatedTextValue4.replaceAll("\\u201c", "");
		String updatedTextValue6 = updatedTextValue5.replaceAll("\\u2019", "");
		String updatedTextValue7 = updatedTextValue6.replaceAll("\\u2026", "");
		String updatedTextValue8 = updatedTextValue7.replaceAll("\\u201d", "");
		String updatedTextValue9 = updatedTextValue8.replaceAll("\\u20e3", "");
		String updatedTextValue10 = updatedTextValue9.replaceAll("'", "");
		String updatedTextValue11 = updatedTextValue10.replaceAll("\\\\", "");
//		logger.debug(updatedTextValue11);
		return "{\"data\" : "+updatedTextValue11+"}";
	}

	public static String[] dividirCadena(String cadena) {
		// Expresión regular para encontrar comas fuera de corchetes cuadrados
		String patron = ",(?![^\\[]*\\])";
		Pattern pattern = Pattern.compile(patron);
		Matcher matcher = pattern.matcher(cadena);

		// Dividir la cadena utilizando la expresión regular
		return pattern.split(cadena);
	}

	private static String reemplazarComasDentroDeComillas(String entrada) {
		StringBuilder resultado = new StringBuilder();
		boolean dentroDeComillas = false;

		for (char c : entrada.toCharArray()) {
			if (c == '\"') {
				dentroDeComillas = !dentroDeComillas;
			}

			if (dentroDeComillas && c == ',') {
				resultado.append('|'); // Reemplaza la coma por un pipe dentro de comillas
			} else {
				resultado.append(c);
			}
		}

		return resultado.toString();
	}

	public static long convierteFechaAUnix(String fechaString) {
		long fechaUnix = 0;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
			Date fechaDate = sdf.parse(fechaString);
			fechaUnix = fechaDate.getTime() / 1000;
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return fechaUnix;
	}

	public static long convertirFechaMinutosAUnix(String fechaString) {
		long unixTimestamp = 0;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			Date fechaDate = sdf.parse(fechaString);
			unixTimestamp = fechaDate.getTime();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return unixTimestamp;
	}
	
	public static String readExcelFromFTP(String ftpServer, String user, String password, String filePath, String rutaCSV) throws IOException {
		List<Data> menciones = new ArrayList<Data>();
		Workbook workbook = null;
		FTPClient ftp = new FTPClient();
//		if (FilesUtil.convertirCSVaExcel(rutaCSV, rutaCSVToExcel)) {
//
//	}
		try {
			ftp.connect(ftpServer);
			ftp.login(user, password);
			ftp.enterLocalPassiveMode();
			ftp.setFileType(FTP.BINARY_FILE_TYPE);

			try (InputStream inputStream = ftp.retrieveFileStream(filePath)) {
				workbook = new XSSFWorkbook(inputStream);
				Sheet tuitsSht = workbook.getSheet("Tuits");
				Sheet usersSht = workbook.getSheet("Usuarios");

				if (tuitsSht != null) {
					for (int i = 1; i <= tuitsSht.getLastRowNum(); i++) {
//						logger.debug("Fila Excel: "+i);
						Row row = tuitsSht.getRow(i);
						Data mencion = new Data();
						User usuario = null;
						Counts countsMencion = new Counts();

						for (int j = 0; j < row.getLastCellNum(); j++) {
							Cell cell = row.getCell(j);
							if (cell != null) {
								if (j == 1) {
//									usuario = getUserByAlias(usersSht, cell.getStringCellValue());
								} else if (j == 2) {
									mencion.setCreatedAt(cell.toString());
								} else if (j == 3) {
									mencion.setText(cell.getStringCellValue());
								} else if (j == 6) { // A partir de aqui son valores de Counts de la mencion
									countsMencion.setRetweets((int) cell.getNumericCellValue());
								} else if (j == 7) {
									countsMencion.setFavorites((int) cell.getNumericCellValue());
								} else if (j == 8) {
									countsMencion.setImpressions((int) cell.getNumericCellValue());
								} else if (j == 9) {
									countsMencion.setQuotes((int) cell.getNumericCellValue());
								} else if (j == 10) {
									countsMencion.setReplies((int) cell.getNumericCellValue());
								} else if (j == 11) {
									countsMencion.setBookmarks((int) cell.getNumericCellValue());
								} else if (j == 12) {
									countsMencion.setTweetValue(Double.valueOf(cell.getStringCellValue()));
								}
							}
						}
						mencion.setUser(usuario);
						mencion.setCounts(countsMencion);
						menciones.add(mencion);
					}
				}
				workbook.close();
			}
		} catch (IOException e) {

			logger.error("Error al lerr FTP: " + e.getMessage());
			e.printStackTrace();
			workbook.close();
		}
		Gson gson = new Gson();
		logger.debug(gson.toJson(menciones));

		return gson.toJson(menciones).toString();
	}

}
