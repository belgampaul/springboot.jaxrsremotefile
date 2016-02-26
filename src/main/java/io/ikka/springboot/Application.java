package io.ikka.springboot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.GET;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

@SpringBootApplication
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
@Controller
class NavigationController {
  @RequestMapping("/index2")
  public String getIndex2() {
    return "index2";
  }

  @RequestMapping(value = "/error2", produces = "application/json")
  public String error() {
    return "error";
  }
}

@RestController
@RequestMapping("/sys")
class SysPropController {
  @GET
  @RequestMapping(path = "/status", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public Response.Status status() {
    return Response.Status.OK;
  }

  @GET
  @RequestMapping(path = "/properties", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public ResponseEntity<Properties> getSystemProperties() {
    return new ResponseEntity<>(System.getProperties(), HttpStatus.OK);
  }

  @GET
  @RequestMapping(path = "/linesStats", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public ResponseEntity<Map<String, Map<String, Long>>> getLinesStats() {
    return new ResponseEntity<>(LinesCounter.getLinesStats(workingDirectory, Arrays.asList(includedFiles.split(",")), Arrays.asList(excludedFiles.split(","))), HttpStatus.OK);
  }

  @Value("${filter.included.files}")
  String includedFiles;

  @Value("${filter.excluded.paths}")
  String excludedFiles;

  @Value("${working.directory}")
  String workingDirectory;



  @GET
  @RequestMapping(path = "/file/create/{name}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public Response.Status touchFile(@PathVariable("name") String fileName) {
    Response.Status res = Response.Status.OK;
    File file = new File("fs/" + fileName);
    try {
      file.mkdirs();
      if (!file.createNewFile()) {
        res = Response.Status.PRECONDITION_FAILED;
      }
    } catch (IOException e) {
      e.printStackTrace();
      res = Response.Status.EXPECTATION_FAILED;
    }
    return res;
  }
}
