package com.centinela.scheduler;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.centinela.scheduler.dto.CreateLiveReport;
import com.centinela.scheduler.dto.RQLogin;
import com.centinela.scheduler.dto.RSLogin;
import com.centinela.scheduler.dto.ReportsId;
import com.centinela.scheduler.dto.ScheduleSP;
import com.centinela.scheduler.dto.Stream;
import com.centinela.scheduler.service.impl.TweetServiceImpl;
import com.centinela.scheduler.util.URLConstants;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;


@Component
public class ExecuteTasks {
	
    public static final Charset UTF_8 = Charset.forName("UTF-8");

	@Autowired
	EntityManager entityManager;

	@Autowired
	RestTemplate restTemplate;
	
	ReportsId[] ids = null;
	
	public ExecuteTasks() {
		this.tweetApiUrl = "";
		this.user = "";
		this.password = "";
	}
	
	@Value("${tweet.url}")
	private final String tweetApiUrl;

	@Value("${tweet.user}")
	private final String user;

	@Value("${tweet.password}")
	private final String password;
	
	static RSLogin token = null;
	
	private final long SEGUNDO = 1000;
	private final long MINUTO = SEGUNDO * 60;
	private final long HORA = MINUTO * 60;
	private final long DIA = HORA * 24;

	
//	@Scheduled(cron = "0 0 03 * * *")
	@Scheduled(fixedRate = DIA)
	public void getLogin() {
		if(token == null) {
			loginTweetSched();
			System.out.println("Token tweet binder: "+token.getAuthToken());
		}
	}
	
	@Scheduled(cron = "0 * * * * ?")
	public void getSchedulers() {
		System.out.println("getSchedulers");
		List<ScheduleSP> scheds = getSchedulersBD();
		//Si la hora y el minuto que trae la consulta coincide con la hora y el minuto actual ejecutar el robot
		if(scheds != null) {
			for(ScheduleSP sched : scheds) {
				boolean ejecuta = compararHorasMinutos(sched.getNext_execution_time());
				if(ejecuta) {
					System.out.println("Ejecutar: "+ejecuta);
				}else {
					System.out.println("Next: "+sched.getNext_execution_time());
				}
			}
		}
		
		
	}
	
	public void ejecutaBot(){
		try {
			
            String rutaPythonScript = "C:\\Users\\bpadi\\Documents\\CODIGO CENTINELA\\bot\\robot.py";

            ProcessBuilder builder = new ProcessBuilder("python", rutaPythonScript);

            builder.redirectErrorStream(true);

            Process proceso = builder.start();

            int retorno = proceso.waitFor();

            if (retorno == 0) {
                System.out.println("El bot se ejecutó correctamente.");
            } else {
                System.out.println("El bot devolvió un error: " + retorno);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
	}
	
	public void refreshReportsId() {
		if(token != null && token.getAuthToken() != "") {
			System.out.println("Refresh ReportIds ");
			getReportsId ();
		}
	}
	
	public void crearReportes() {
		if(token != null && token.getAuthToken() != "") {
			getReportsId ();
			TweetServiceImpl tweetService = new TweetServiceImpl(restTemplate, entityManager);
			CreateLiveReport parametros = null;
			Stream stream = null;
			if(ids != null) {
				for (ReportsId repId : ids) {
					parametros = new CreateLiveReport();
					parametros = repId.getQueryreport();
					System.out.println("Creando nuevo reporte del poyecto con ID: " + parametros.getIdProyecto());
					stream = new Stream();
					stream.setStartDate(repId.getQueryreport().getStream().getEndDate());//Se coloca la fecha final del reporte anterior
					stream.setEndDate(obtenerFechaActualEnUnix());//Se coloca la fecha actual a las 11 am que es la hora en que se ejecuta
					parametros.setStream(stream);
					String respuesta = tweetService.createLiveReport(parametros, tweetApiUrl);
					System.out.println(respuesta);
				}
			}
		}
	}

	public List<ScheduleSP> getSchedulersBD() {
		
		StoredProcedureQuery storedProcedureQuery = entityManager.createStoredProcedureQuery("centineladb.sp_getSchedules");
		storedProcedureQuery.registerStoredProcedureParameter("response", String.class, ParameterMode.OUT);

		storedProcedureQuery.execute();
		
		String response = (String)storedProcedureQuery.getOutputParameterValue("response");
		
		System.out.println(response);
		
		Gson gson = new Gson();
        
		Type listTypeSched = new TypeToken<List<ScheduleSP>>() {}.getType();
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

		ResponseEntity<RSLogin> responseEntity = restTemplate.exchange(tweetApiUrl + URLConstants.URL_LOGIN, HttpMethod.POST, requestEntity, RSLogin.class);

		token = responseEntity.getBody();
		

	}
	
	public void getReportsId () {

		Gson gson = new Gson();
		
		StoredProcedureQuery storedProcedureQuery = entityManager.createStoredProcedureQuery("centineladb.sp_getActiveQueryReport");
		storedProcedureQuery.registerStoredProcedureParameter("response", String.class, ParameterMode.OUT);
        storedProcedureQuery.execute();
        
        String respuesta = (String) storedProcedureQuery.getOutputParameterValue("response");
                
        ids = gson.fromJson(respuesta,ReportsId[].class);

		System.out.println(respuesta);

	}
	
	public static String dameToken(){
		
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

}
