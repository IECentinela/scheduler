package com.centinela.scheduler;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.centinela.scheduler.dto.CreateLiveReport;
import com.centinela.scheduler.dto.Query;
import com.centinela.scheduler.dto.RQLogin;
import com.centinela.scheduler.dto.RSLogin;
import com.centinela.scheduler.dto.ReportsId;
import com.centinela.scheduler.dto.ScheduleSP;
import com.centinela.scheduler.dto.Stream;
import com.centinela.scheduler.service.impl.TweetServiceImpl;
import com.centinela.scheduler.util.FilesUtil;
import com.centinela.scheduler.util.URLConstants;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@Component
@Service
public class ExecuteTasks {
	
	private static final Logger logger = LoggerFactory.getLogger(ExecuteTasks.class);
	
	public static final Charset UTF_8 = Charset.forName("UTF-8");

	@Autowired
	EntityManager entityManager;

	@Autowired
	RestTemplate restTemplate;

	public ExecuteTasks() {
		this.tweetApiUrl = "";
		this.user = "";
		this.password = "";
		this.serverFTP = "";
		this.portFTP = 0;
		this.usernameFTP = "";
		this.passwordFTP = "";
	}

	@Value("${tweet.url}")
	private final String tweetApiUrl;

	@Value("${tweet.user}")
	private final String user;

	@Value("${tweet.password}")
	private final String password;

	@Value("${ftp.serverFTP}")
	private final String serverFTP;

	@Value("${ftp.portFTP}")
	private final int portFTP;

	@Value("${ftp.usernameFTP}")
	private final String usernameFTP;

	@Value("${ftp.passwordFTP}")
	private final String passwordFTP;

	static RSLogin token = null;

	private final long SEGUNDO = 1000;
	private final long MINUTO = SEGUNDO * 60;
	private final long HORA = MINUTO * 60;
	private final long DIA = HORA * 24;

	@Scheduled(cron = "0 0 7 * * ?")
	public void getLogin() {
//		if (token == null) {
			loginTweetSched();
			logger.info("Token tweet binder: " + token.getAuthToken());
//		}
	}

	// TODO Generar tarea que se ejecute todos los días a las 23:59 para lectura y
	// procesamiento de excel y CSV mediante la ruta de carpetas
//	@Scheduled(cron = "0 * * * * ?")
	@Scheduled(cron = "0 0 16 * * ?")
	public void cargaDatosContadores() throws IOException {

		List<String> rutas = generaRutasFTP();
		
		for (String ruta : rutas) {
			String[] valores = ruta.split(",");
			logger.debug(valores[0] + " ID: " + valores[1]);

//			String remoteFilePath = "/informes-centinela/109_Claudia Sheinbaum/2023/11.NOV/23/graphext_X_21.csv";
			String remoteFilePath = valores[0];
			String localFilePath = "graphext_clean.csv";

			// Descargar el archivo CSV desde el servidor FTP
//			ModificarArchivoCSV.downloadCsvFromFTP(serverFTP, portFTP, usernameFTP, usernameFTP, remoteFilePath,localFilePath);
//			ModificarArchivoCSV.processCSV(localFilePath);
//			ModificarArchivoCSV.uploadFile(serverFTP, portFTP, usernameFTP, usernameFTP, remoteFilePath, localFilePath);

			String localExcelFilePath = "archivo.xlsx";
			FilesUtil.downloadCsvFromFTP(serverFTP, portFTP, usernameFTP, passwordFTP,valores[0], localExcelFilePath);
			String localExcelFileConvertedPath = FilesUtil.convertCsvToExcel(localExcelFilePath);
			String remoteExcelFilePath = valores[0].replace(".csv", ".xlsx");
			FilesUtil.uploadFileToFTP(serverFTP, portFTP, usernameFTP, passwordFTP, remoteExcelFilePath,localExcelFileConvertedPath);
			String jsonGT = FilesUtil.procesaExcelGraphtext(serverFTP, usernameFTP, passwordFTP, remoteExcelFilePath);
//			logger.debug(jsonGT);
			try {
				String guardar = save7Days(jsonGT, valores[1]);
				logger.debug(guardar);
			}catch(Exception e) {
				logger.error("Error al ejecutar save7Days: "+ruta);
				logger.error(e.getMessage());
			}
			

		}
		
//		sp_updateLocationUser();

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
	
	@Scheduled(cron = "0 35 16 * * ?")
	public void sp_updateLocationUser() {
		logger.debug("Update location user...");
		try {
			StoredProcedureQuery storedProcedureQuery = entityManager.createStoredProcedureQuery("centineladb.sp_updateLocationUser");
	        storedProcedureQuery.execute();
		}catch(Exception e) {
			logger.debug("Error al ejecutar Get Location: "+e.getMessage());
			e.printStackTrace();
		}
		
	}

	public List<String> generaRutasFTP() {
		List<String> rutas = new ArrayList<String>();
		List<ScheduleSP> scheds = getSchedulersBD();
		String ruta = "";
		for (ScheduleSP sched : scheds) {
			if (sched.getFk_source() == (int) 1) {
				int dia = LocalDate.now().getDayOfMonth();
				String numeroString = Integer.toString(dia);
				if(numeroString.length() == 1) {
					numeroString = "0"+numeroString;
				}
				ruta = "/informes-centinela/" + sched.getFk_project().toString() + "_" + sched.getProjectName() + "/"
						+ Year.now().getValue() + "/" + obtenerMesActual() + "/" + numeroString
						+ "/graphext.csv," + sched.getResourceId();
				rutas.add(ruta);
				logger.debug(ruta);
			}
		}
		return rutas;
	}

	public static String obtenerMesActual() {
		LocalDate fechaActual = LocalDate.now();
		int numeroMes = fechaActual.getMonthValue();
		String numMesString = Integer.toString(numeroMes);
		if(numMesString.length() == 1) {
			numMesString = "0"+numMesString;
		}
		String nombreMes = fechaActual.getMonth().getDisplayName(TextStyle.SHORT, new Locale("es", "ES")).toUpperCase();
		String resultado = numMesString + "." + nombreMes.replace(".", "");
		return resultado;
	}

	@Scheduled(cron = "0 * * * * ?")
	public void getSchedulers() {
		logger.debug("getSchedulers");
		List<ScheduleSP> scheds = getSchedulersBD();
		// Si la hora y el minuto que trae la consulta coincide con la hora y el minuto
		// actual
		// ejecutar creación de reporte y traer estadisticas
		if (scheds != null) {
			for (ScheduleSP sched : scheds) {
				if (sched.getFk_source() == 1) {
					boolean ejecuta = compararHorasMinutos(sched.getNext_execution_time());
					if (ejecuta) {

						logger.debug("Ejecutando tarea: " + ejecuta + "ID: " + sched.getFk_project());
						List<ReportsId> queryesReport = getReportsIdByIdProject(sched.getFk_project());

						for (ReportsId query : queryesReport) {
							logger.debug("query.getFk_project(): " + query.getFk_project());
							logger.debug("sched.getFk_project(): " + sched.getFk_project());
							logger.debug("query.getFk_source(): " + query.getFk_source());
							logger.debug("Comparacion IDs: "+query.getFk_project().compareTo(sched.getFk_project()));
							logger.debug("Comparacion Source: "+query.getFk_source().compareTo(1));
							if ((query.getFk_project().compareTo(sched.getFk_project()) == 0) && (query.getFk_source().compareTo(1) == 0)) {// Deben coincidir IDProyecto y IDSource = 1
								logger.debug("Creando reporte del proyecto con ID: " + query.getFk_project() + " y source: " + query.getFk_source() + " con el query: "+ query.getQueryreport().getQuery());

								crearReporte(query);
								
								ScheduledExecutorService executor1 = Executors.newSingleThreadScheduledExecutor();
								executor1.schedule(() -> {
									logger.debug("Actualizando Schedule..."+sched.getIDExecution());
									actualizaNextExecution(sched.getIDExecution());
								}, 10, TimeUnit.SECONDS);
								executor1.shutdown();

								ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
								executor.schedule(() -> {
									logger.debug("Ejecutando getStats...");
									// Consultar de nuevo los queryreport para traer el ultimo que corresponde el
									// proyecto con el id que estamos ejecutando
									List<ReportsId> queryesReport2 = getReportsIdByIdProject(sched.getFk_project());
									for (ReportsId query2 : queryesReport2) {
										if ((query2.getFk_project().compareTo(sched.getFk_project()) == 0) && (query2.getFk_source().compareTo(1) == 0)) {
											TweetServiceImpl tweetService = new TweetServiceImpl(restTemplate, entityManager);
											logger.debug("ID de proyecto: "+query2.getFk_project().toString());
											logger.debug("QueryReport: "+query2.getResourceId());
											String statsResponse = tweetService.getStats(query2.getFk_project(), query2.getResourceId(), tweetApiUrl);
											logger.debug(statsResponse.toString());
										}
									}
								}, 5, TimeUnit.MINUTES);
								executor.shutdown();
							}else {
								logger.info("El reporte con ID: "+sched.getFk_project()+" no se genero...");
							}
						}

					} else {
						logger.debug("Next: " + sched.getNext_execution_time() + " , IDProyecto: " + sched.getFk_project() + " , IDExecution: " + sched.getIDExecution());
					}
				}
			}
		}
	}

	public void actualizaNextExecution(Integer idExecution) {

		StoredProcedureQuery storedProcedureQuery = entityManager
				.createStoredProcedureQuery("centineladb.sp_updateNextExecution");
		storedProcedureQuery.registerStoredProcedureParameter("in_IDExecution", Integer.class, ParameterMode.IN);
		storedProcedureQuery.registerStoredProcedureParameter("response", String.class, ParameterMode.OUT);

		storedProcedureQuery.setParameter("in_IDExecution", idExecution);

		storedProcedureQuery.execute();

		String response = (String) storedProcedureQuery.getOutputParameterValue("response");
//		logger.debug("Response sp_updateNextExecution: " + response);
		if (response != null) {
			logger.debug("IDExecution: " + idExecution);
			logger.debug("Response sp_updateNextExecution: " + response);
		}
	}

	public void crearReporte(ReportsId id) {
		logger.debug("Token crear reporte: "+token);
		
		if(token == null || token.getAuthToken() == "") {
			logger.debug("Token null, generando nuevo...");
			getLogin();
			logger.debug("Nuevo token crear reporte: "+token);
		}
		
		if (token != null && token.getAuthToken() != "") {
			TweetServiceImpl tweetService = new TweetServiceImpl(restTemplate, entityManager);
			CreateLiveReport parametros = null;
			Stream stream = null;
			Query newQueryMust = null;
			if (id != null) {
				parametros = new CreateLiveReport();
				parametros = id.getQueryreport();
				newQueryMust = parametros.getQuery();
				// Sacar arreglo de must para recorrerlo y actualizar fechas
				List<String> musts = newQueryMust.getMust();
				List<String> newMust = new ArrayList<String>();

				for (String val : musts) {
					String[] separa = val.split(",");
					
					for(String valor : separa) {
						if(valor.equals("-is:retweet")) {
							newMust.add(valor);
						}else if(valor.contains("since:")){
							newMust.add("since:"+fechaHoy());
						}else if(valor.contains("until:")){
							logger.debug("Se elimina ---"+valor+"--- del query del proyecto "+id.getFk_project());
						}else {
							newMust.add(valor);
						}
					}
////					if (separa[0].equals("until")) {
////						newMust.add("since:" + separa[1]);
////					} else 
//					if (separa[0].equals("since")) {
//						logger.debug(val);
//					} else {
//						newMust.add(val);
//					}

				}
				// En i+1 setear el until
//				newMust.add("since:" + fechaHoy());
				newQueryMust.setMust(newMust);
				parametros.setQuery(newQueryMust);
				parametros.setStartDateProject(id.getStartDateProject());
				parametros.setEndDateProject(id.getEndDateProject());
				parametros.setIdProyecto(id.getFk_project());
//				logger.debug(parametros.getQuery());
				logger.debug("Creando nuevo reporte del poyecto con ID: " + parametros.getIdProyecto());
				String respuesta = tweetService.createLiveReport(parametros, tweetApiUrl);
				logger.debug("Respuesta crearReporte: " + respuesta);
			}
		}else {
			logger.debug("Reporte no generado con ID: "+id.getFk_project());
		}
	}

	public static String fechaHoy() {
		// Obtener la fecha del día de hoy
		LocalDate fechaHoy = LocalDate.now();

		// Formatear la fecha en el formato deseado
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		return fechaHoy.format(formatter);
	}

	public List<ScheduleSP> getSchedulersBD() {

		StoredProcedureQuery storedProcedureQuery = entityManager
				.createStoredProcedureQuery("centineladb.sp_getSchedules");
		storedProcedureQuery.registerStoredProcedureParameter("response", String.class, ParameterMode.OUT);

		storedProcedureQuery.execute();

		String response = (String) storedProcedureQuery.getOutputParameterValue("response");

		if (response != null) {
			logger.debug(response);
		}

		Gson gson = new Gson();

		Type listTypeSched = new TypeToken<List<ScheduleSP>>() {
		}.getType();
		List<ScheduleSP> scheds = gson.fromJson(response, listTypeSched);

		return scheds;
	}

	public void loginTweetSched() {

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		RQLogin loguear = new RQLogin();

		loguear.setEmail(user);
		loguear.setPassword(password);

		Gson gson = new Gson();
		String body = gson.toJson(loguear);

		HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);

		ResponseEntity<RSLogin> responseEntity = restTemplate.exchange(tweetApiUrl + URLConstants.URL_LOGIN,
				HttpMethod.POST, requestEntity, RSLogin.class);

		token = responseEntity.getBody();

	}

	public ReportsId[] getReportsId() {
		logger.debug("Ejecutando: getReportsId");
		Gson gson = new Gson();

		StoredProcedureQuery storedProcedureQuery = entityManager.createStoredProcedureQuery("centineladb.sp_getActiveQueryReport");
		storedProcedureQuery.registerStoredProcedureParameter("response", String.class, ParameterMode.OUT);
		storedProcedureQuery.execute();

		String respuesta = (String) storedProcedureQuery.getOutputParameterValue("response");

		ReportsId[] ids = null;
		ids = gson.fromJson(respuesta, ReportsId[].class);
		logger.debug(ids.toString());
		return ids;
	}

	public static String dameToken() {
		return token.getAuthToken();
	}

	public static long obtenerFechaActualEnUnix() {
		// Obtener la fecha y hora actual
		Date fechaActual = new Date();

		// Crear un formato SimpleDateFormat para formatear la fecha
		SimpleDateFormat formatoFecha = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		// Formatear la fecha en "YYYY-MM-DD HH:mm:ss"
		String fechaFormateada = formatoFecha.format(fechaActual);

		// Convertir la fecha formateada a un objeto Date
		try {
			Date fechaFormateadaDate = formatoFecha.parse(fechaFormateada);

			// Obtener el timestamp Unix (en milisegundos)
			long timestampUnix = fechaFormateadaDate.getTime() / 1000;

			return timestampUnix;
		} catch (Exception e) {
			e.printStackTrace();
			return -1; // En caso de error
		}
	}

	public static boolean compararHorasMinutos(String fechaIngresada) {
		SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
		Date fechaActual = new Date();
		try {
			Date fechaParametro = formato.parse(fechaIngresada);
			int horasFechaActual = fechaActual.getHours();
			int minutosFechaActual = fechaActual.getMinutes();
			int horasFechaParametro = fechaParametro.getHours();
			int minutosFechaParametro = fechaParametro.getMinutes();

			return horasFechaActual == horasFechaParametro && minutosFechaActual == minutosFechaParametro;
		} catch (ParseException e) {
			e.printStackTrace();
			return false; // En caso de error en el formato de fecha
		}
	}

	public List<ReportsId> getReportsIdByIdProject(int idProject) {
		logger.debug("getReportsById...");
		List<ReportsId> respuesta = new ArrayList<ReportsId>();

		ReportsId[] ids = getReportsId();

		for (ReportsId id : ids) {
			if (id.getFk_project() == idProject && id.getFk_source() == 1) {
				respuesta.add(id);
			}
		}

		return respuesta;
	}

}
