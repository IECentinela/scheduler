package com.centinela.scheduler.util;

import java.lang.reflect.Type;
import java.text.DateFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.persistence.EntityManager;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.centinela.scheduler.dto.FTPCredentials;
import com.centinela.scheduler.dto.ScheduleSP;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


@Component
public class GeneraReportesManual {
	
	private static final Logger logger = LoggerFactory.getLogger(GeneraReportesManual.class);
	
	private final
	EntityManager entityManager;

	@Autowired
	RestTemplate restTemplate;
	
	public GeneraReportesManual(EntityManager entityManager) {
		this.entityManager = entityManager;
		this.serverFTP = "";
		this.portFTP = 0;
		this.usernameFTP = "";
		this.passwordFTP = "";
	}
	
	@Value("${ftp.serverFTP}")
	private final String serverFTP;

	@Value("${ftp.portFTP}")
	private final int portFTP;

	@Value("${ftp.usernameFTP}")
	private final String usernameFTP;

	@Value("${ftp.passwordFTP}")
	private final String passwordFTP;
	
	public String cargarReportesPorProyectoFechas(String fechaInicial, String fechaFinal, int idProyecto, FTPCredentials ftp) {
		
		String respuesta = null;
		try {
			
			List<String> rutas = generarURLs(fechaInicial, fechaFinal, idProyecto);
			
			for (String ruta : rutas) {
				
				String[] valores = ruta.split(",");
				logger.debug(valores[0] + " ID: " + valores[1]);

				String localExcelFilePath = "archivo.xlsx";
				FilesUtil.downloadCsvFromFTP(ftp.getServerFTP(), ftp.getPortFTP(), ftp.getUsernameFTP(), ftp.getPasswordFTP(),valores[0], localExcelFilePath);
				String localExcelFileConvertedPath = FilesUtil.convertCsvToExcel(localExcelFilePath);
				String remoteExcelFilePath = valores[0].replace(".csv", ".xlsx");
				FilesUtil.uploadFileToFTP(ftp.getServerFTP(), ftp.getPortFTP(), ftp.getUsernameFTP(), ftp.getPasswordFTP(), remoteExcelFilePath,localExcelFileConvertedPath);
				String jsonGT = FilesUtil.procesaExcelGraphtext(ftp.getServerFTP(), ftp.getUsernameFTP(), ftp.getPasswordFTP(), remoteExcelFilePath);

				try {
					String guardar = save7Days(jsonGT, valores[1]);
					logger.debug(guardar);
				}catch(Exception e) {
					respuesta = "{\"response\" : \"300\"}";
					logger.error("Error al ejecutar save7Days: "+ruta);
					logger.error(e.getMessage(),e);
				}
				respuesta = "{\"response\" : \"200\"}";
			}
			
		}catch(Exception ex) {
			respuesta = "{\"response\" : \"301\"}";
			logger.error("Error al ejecutar cargarReportesPorProyectoFechas");
			logger.error(ex.getMessage(),ex);
		}
		
		return respuesta;
		
	}
	
	public static String obtenerMesActual() {
		LocalDate fechaActual = LocalDate.now();
		int numeroMes = fechaActual.getMonthValue();
		String nombreMes = fechaActual.getMonth().getDisplayName(TextStyle.SHORT, new Locale("es", "ES")).toUpperCase();
		String resultado = numeroMes + "." + nombreMes.replace(".", "");
		return resultado;
	}
	
	private List<ScheduleSP> getSchedulersBD() {

		StoredProcedureQuery storedProcedureQuery = entityManager.createStoredProcedureQuery("centineladb.sp_getSchedules");
		storedProcedureQuery.registerStoredProcedureParameter("response", String.class, ParameterMode.OUT);

		storedProcedureQuery.execute();

		String response = (String) storedProcedureQuery.getOutputParameterValue("response");

		if (response != null) {
			logger.debug(response);
		}

		Gson gson = new Gson();
		Type listTypeSched = new TypeToken<List<ScheduleSP>>() {}.getType();
		List<ScheduleSP> scheds = gson.fromJson(response, listTypeSched);

		return scheds;
	}
	
	public String save7Days(String json, String reportId) {
		StoredProcedureQuery storedProcedureQuery = entityManager
				.createStoredProcedureQuery("centineladb.sp_processData7days");
		storedProcedureQuery.registerStoredProcedureParameter("in_jsonData", String.class, ParameterMode.IN);
		storedProcedureQuery.registerStoredProcedureParameter("in_resourceId", String.class, ParameterMode.IN);
		storedProcedureQuery.registerStoredProcedureParameter("response", String.class, ParameterMode.OUT);

		storedProcedureQuery.setParameter("in_jsonData", json);
		storedProcedureQuery.setParameter("in_resourceId", reportId);
		storedProcedureQuery.execute();

		String respuesta = (String) storedProcedureQuery.getOutputParameterValue("response");

		return respuesta;
	}
	
	public List<String> generarURLs(String fechaInicio, String fechaFinal, Integer idProyecto) {
        
		List<String> urls = new ArrayList<>();
        List<ScheduleSP> scheds = getSchedulersBD();
		String nombreProyecto = "";
		String idReporte = "";
		
		for (ScheduleSP sched : scheds) {
			if (sched.getFk_source() == (int) 1 && sched.getFk_project() == idProyecto) {
				nombreProyecto = sched.getProjectName();
				idReporte = sched.getResourceId();
			}
		}

        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate fechaInicioLocalDate = LocalDate.parse(fechaInicio, inputFormatter);
        LocalDate fechaFinalLocalDate = LocalDate.parse(fechaFinal, inputFormatter);

        while (!fechaInicioLocalDate.isAfter(fechaFinalLocalDate)) {
            String url = construirURL(fechaInicioLocalDate, idProyecto, nombreProyecto, idReporte);
            urls.add(url);
            fechaInicioLocalDate = fechaInicioLocalDate.plusDays(1);
        }
        return urls;
    }

    private static String construirURL(LocalDate fecha, Integer idProyecto, String nombreProyecto, String idReporte) {
        String nombreMes = obtenerNombreMes(fecha);
        String numeroString = Integer.toString(fecha.getDayOfMonth());
		if(numeroString.length() == 1) {
			numeroString = "0"+numeroString;
		}
		String numMesString = Integer.toString(fecha.getMonthValue());
		if(numMesString.length() == 1) {
			numMesString = "0"+numMesString;
		}
        String urlFormato = "/informes-centinela/%d_%s/%s/%s/%s/graphext.csv,%s";
        return String.format(urlFormato, idProyecto, nombreProyecto, fecha.getYear(), numMesString+"."+nombreMes.toUpperCase(), numeroString,idReporte);
    }
    
    public static String obtenerNombreMes(LocalDate fecha) {

        DateFormatSymbols symbols = new DateFormatSymbols(new Locale("es", "ES"));
        String[] nombresMeses = symbols.getMonths();
        int indiceMes = fecha.getMonthValue() - 1;
        return nombresMeses[indiceMes].substring(0, 3).toUpperCase();
    }

//    public static void main(String[] args) {
//        // Ejemplo de uso
//        String fechaInicio = "2023-12-25";
//        String fechaFinal = "2024-01-10";
//        Integer idProyecto = 109;
//
//        List<String> urls = generarURLs(fechaInicio, fechaFinal, idProyecto);
//
//        // Imprimir las URLs generadas
//        for (String url : urls) {
//            System.out.println(url);
//        }
//    }

}
