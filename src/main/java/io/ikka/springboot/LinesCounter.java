package io.ikka.springboot;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author ikka
 * @date: 26.02.2016.
 */
public class LinesCounter {


  private static final String LINES_COUNTER_BAT = "linesCount.bat";
  private static final String tempDataFileName = "data";
  private static final String MANIFEST_GRADLE_PATH = "manifest.gradle";
  private static final String COUNT_LINES_SCRIPT = "" +
      "git ls-files | xargs wc -l >> %s\n" +
      "exit";


  public static Map<String, Map<String, Long>> getLinesStats(String workingDirectory, List<String> includedFileTypes, List<String> excludedFiles) {
    Binding binding = new Binding();
    GroovyShell shell = new GroovyShell(binding);
    final Pattern includedFileTypesPattern = Pattern.compile(String.format("\\.(%s)$", StringUtils.join(includedFileTypes, "|")));
    final Pattern excludedFileTypesPattern = Pattern.compile(String.format("^(%s)", StringUtils.join(excludedFiles, "|")));

    Map<String, Long> totalsForProject = new HashMap<>();
    Map<String, Map<String, Long>> dataPerModule = new HashMap<>();
    try {
      Script scrpt = shell.parse(new File(workingDirectory + "/" + MANIFEST_GRADLE_PATH));
      scrpt.run();

      //noinspection unchecked
      ArrayList<LinkedHashMap<String, String>> modules = (ArrayList<LinkedHashMap<String, String>>) ((LinkedHashMap) scrpt.getProperty("product")).get("modules");
      addFinroot(modules);

      for (LinkedHashMap<String, String> module : modules) {
        String _path = String.valueOf(module.get("path"));
        //        System.out.println(_path);

        dataPerModule.put(_path, countLinesForModule(workingDirectory + "/" + _path, LINES_COUNTER_BAT, tempDataFileName, includedFileTypesPattern, excludedFileTypesPattern));
        //        System.out.println();
      }

      for (Map.Entry<String, Map<String, Long>> moduleStats : dataPerModule.entrySet()) {
        Map<String, Long> value = moduleStats.getValue();
        for (Map.Entry<String, Long> totalsPerType : value.entrySet()) {
          String key = totalsPerType.getKey();
          Long valuePerTypeAndModule = totalsPerType.getValue();
          if (totalsForProject.containsKey(key)) {
            Long aLong = totalsForProject.get(key);
            totalsForProject.put(key, aLong + valuePerTypeAndModule);
          } else {
            totalsForProject.put(key, valuePerTypeAndModule);
          }
        }

      }
      //      System.out.println("Grand Totals:");
      //      for (Map.Entry<String, Long> totalPerType : totalsForProject.entrySet()) {
      //        System.out.println(String.format("%s %s", totalPerType.getKey(), totalPerType.getValue()));
      //      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    dataPerModule.put("totals", totalsForProject);
    return dataPerModule;
  }


  /**
   * add finroot to modules
   * @param modules
   */
  protected static void addFinroot(ArrayList<LinkedHashMap<String, String>> modules) {
    LinkedHashMap<String, String> finrootMap = new LinkedHashMap<>();
    finrootMap.put("path", ".");
    modules.add(finrootMap);
  }

  protected static Map<String, Long> countLinesForModule(String workingDirectoryPath, String batFileName, String tempDataFileName, Pattern includedFiles, Pattern excludedFiles) {
    Process exec = null;
    Map<String, Long> statsByType = new HashMap<>();
    try {

      Runtime runtime = Runtime.getRuntime();
      String command = String.format(COUNT_LINES_SCRIPT, tempDataFileName);

      Path batFilePath = FileSystems.getDefault().getPath(workingDirectoryPath + "/" + batFileName);
      Files.write(batFilePath, command.getBytes(), StandardOpenOption.CREATE); // create new, overwrite if exists

      exec = runtime.exec("cmd /c start /wait " + batFileName, null, new File(workingDirectoryPath));

      try {
        System.out.println("Waiting for batch file exection...");
        exec.waitFor();
        System.out.println("Batch file executed.");

        Path dataFilePath = FileSystems.getDefault().getPath(workingDirectoryPath + "/" + tempDataFileName);
        List<String> lines = Files.readAllLines(dataFilePath, Charset.defaultCharset());
        long totalLinesCnt = 0;
        for (String line : lines) {
          String[] splittedLine = line.trim().split(" ");
          long linesCount = Long.parseLong(splittedLine[0]);
          String fileName = splittedLine[1];

          if (includedFiles.matcher(fileName).find() && !excludedFiles.matcher(fileName).find()) {
            String extension = FilenameUtils.getExtension(fileName);
            if (statsByType.containsKey(extension)) {
              Long aLong = statsByType.get(extension);
              statsByType.put(extension, aLong + linesCount);
            } else {
              statsByType.put(extension, linesCount);
            }
            //            System.out.println(line);
            totalLinesCnt += linesCount;
          }


        }
        totalLinesCnt = 0;
        Set<Map.Entry<String, Long>> entries = statsByType.entrySet();
        for (Map.Entry<String, Long> entry : entries) {
          Long linesCount = entry.getValue();
          totalLinesCnt += linesCount;
          //          System.out.println(String.format("%s %s", entry.getKey(), linesCount));
        }
        Files.delete(dataFilePath);
        Files.delete(batFilePath);
        //        System.out.println(totalLinesCnt);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (exec != null) {
        exec.destroy();
      }
    }
    return statsByType;
  }

}

class ProcessOutputDigester {
  public static String digestToString(Process process) throws IOException {
    StringBuilder builder = new StringBuilder();

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        builder.append(line);
        builder.append(System.getProperty("line.separator"));
      }
    }

    return builder.toString();
  }

  public static List<String> digestToStringList(Process process) throws IOException {
    ArrayList<String> res = new ArrayList<>();

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        res.add(line);
      }
    }
    return res;
  }
}

class ProcessFactory {

  protected static Process getProcess(String workingDirectoryPath, String... command) throws IOException {
    ProcessBuilder processBuilder = new ProcessBuilder(command);
    processBuilder.directory(new File(workingDirectoryPath));
    return processBuilder.start();
  }
}
