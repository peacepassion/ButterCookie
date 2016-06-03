package me.ele

import com.android.build.gradle.api.BaseVariantOutput
import com.android.build.gradle.api.LibraryVariant
import org.apache.commons.io.FileUtils
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.lang.reflect.Field

class ButterCookie implements Plugin<Project> {

  static final String[] ARRAY = ["@OnClick(",
                                 "@OnCheckedChanged(",
                                 "@OnEditorAction(",
                                 "@OnFocusChange(",
                                 "@OnItemClick(",
                                 "@OnItemLongClick(",
                                 "@OnItemSelected(",
                                 "@OnLongClick(",
                                 "@OnPageChange(",
                                 "@OnTextChanged(",
                                 "@OnTouch(",
                                 "@BindArray(",
                                 "@BindBitmap(",
                                 "@BindBool(",
                                 "@BindColor(",
                                 "@BindDimen(",
                                 "@BindDrawable(",
                                 "@BindInt(",
                                 "@BindString(",
                                 "@BindView(",
                                 "@BindViews("]

  private Project project
  private String packageName

  void apply(Project project) {

    this.project = project
    if (!project.hasProperty('android')) {
      throw new GradleException('Please apply the Android plugin first')
    }

    project.afterEvaluate {

      if (!project.plugins.findPlugin('com.android.library')) {
        throw new GradleException('This plugin is just for android library plugin!!!')
      }

      project.android.libraryVariants.all { LibraryVariant variant ->

        variant.outputs.each { BaseVariantOutput output ->
          output.processResources.doLast {
            //change all fields in R.class to final
            changeAllFieldsOfIdToFinal(variant)
          }
        }

        boolean isJavaCompiled = false
        variant.javaCompiler.doLast {
          if (isJavaCompiled) {
            return
          }
          isJavaCompiled = true

          //copy all sources & $$ViewBinder into build, make sourceSet main point this directory
          copyAllSourcesToBuild(variant)

          //delete ButterKnife related code in sources, exclude ButterKnife dependency
          deleteAnnotationsFromSource(variant)

          //find all $$ViewBinder ids, replace them with R.**.**, change all R.class fields to non-final
          deleteAllFinalInIds(variant)
          replaceIdsInViewBinders(variant)

          //recompile source code
          recompileSourceCode(variant)

          //compile & package, but you needn't do anything
        }
      }
    }
  }

  void recompileSourceCode(LibraryVariant variant) {
    variant.javaCompiler.source.files.clear()
    Field sourceField = Class.forName("org.gradle.api.tasks.SourceTask").getDeclaredField("source")
    sourceField.setAccessible(true)
    List<Object> objs = sourceField.get(variant.javaCompiler)
    objs.remove(0)
    objs.add(0, new File(getCopiedSource(variant)))
    objs.add(project.file(getApt(variant)))

    variant.javaCompiler.state.executed = false
    variant.javaCompiler.execute()
  }

  void deleteAnnotationsFromSource(LibraryVariant variant) {
    String sourcePath = getCopiedSource(variant)
    List<File> files = getAllSourceFiles(sourcePath)
    files.each { File file ->
      StringBuilder content = new StringBuilder()
      file.eachLine { String line ->
        content.append(getLineAfterDeletion(line))
        content.append('\n')
      }
      file.delete()
      file << content.toString()
    }
  }

  String getLineAfterDeletion(String line) {
    ARRAY.each { String item ->
      if (line.contains(item)) {
        int startIndex = line.indexOf(item)
        int endIndex = line.indexOf(item) + item.length() + line.substring(startIndex + item.length()).indexOf(")")
        line = line.substring(0, startIndex) + line.substring(endIndex + 1)
      }
    }

    return line
  }

  void replaceIdsInViewBinders(LibraryVariant variant) {
    String aptDirPath = "${project.buildDir.absolutePath}/generated/source/apt/${variant.name}"
    List<File> viewBinderFiles = getAllViewBinders(aptDirPath)
    Map<String, String> mapping = getMappings(variant)
    StringBuilder content = new StringBuilder()
    viewBinderFiles.each { File file ->
      file.eachLine { String line ->
        boolean isMatched = false
        mapping.each { k, v ->
          if (line.contains(k) && !line.contains("view${k}")) {
            content.append(line.replace(k, mapping.get(k)))
            isMatched = true
          }
        }
        if (!isMatched) {
          content.append(line)
        }
        content.append('\n')
      }
      file.delete()
      file << content.toString()
    }
  }

  Map<String, String> getMappings(LibraryVariant variant) {
    Map<String, String> map = new HashMap<>()
    String rFilePath = getR(variant)

    String subRes = ""

    project.file(rFilePath).eachLine { line ->

      if (line.contains("public static final class")) {
        String[] arrayOfStrings = line.trim().split(" ")
        subRes = arrayOfStrings[arrayOfStrings.length - 2]
      }

      if (line.contains("public static int") && !line.contains("public static int[]")) {
        try {
          String[] segments = line.trim().split(" ")
          String[] validInfos = segments[segments.length - 1].trim().split("=")
          String key = "${Integer.parseInt(validInfos[1].substring(2, validInfos[1].length() - 1), 16)}"
          String value = "${findPackageName()}.R.${subRes}.${validInfos[0]}"
          map.put(key, value)
        } catch (Exception e) {
          //ignore invalid line
        }
      }
    }

    return map
  }

  void deleteAllFinalInIds(LibraryVariant variant) {
    String rFilePath = getR(variant)
    StringBuilder content = new StringBuilder()
    project.file(rFilePath).eachLine { line ->
      content.append(line.replace("public static final int", "public static int"))
      content.append('\n')
    }

    project.file(rFilePath).delete()
    project.file(rFilePath) << content.toString()
  }

  List<File> getAllViewBinders(String dirPath) {
    File aptDir = new File(dirPath)
    List<File> list = new ArrayList<>()
    aptDir.listFiles().each { File file ->
      if (file.isFile() && file.name.endsWith("\$\$ViewBinder.java")) {
        list.add(file)
      }

      if (file.isDirectory()) {
        list.addAll(getAllViewBinders(file.absolutePath))
      }
    }

    return list
  }

  void copyAllSourcesToBuild(LibraryVariant variant) {
    Collection<File> mainJavaDirectories = project.android.sourceSets.main.javaDirectories
    mainJavaDirectories.each { File sourceDir ->
      File buildSourceDir = new File(getCopiedSource(variant))
      buildSourceDir.delete()
      buildSourceDir.mkdirs()
      FileUtils.copyDirectory(sourceDir, buildSourceDir)
      mainJavaDirectories.remove(sourceDir)
      mainJavaDirectories.add(buildSourceDir)
    }
  }

  void changeAllFieldsOfIdToFinal(LibraryVariant variant) {
    String rFilePath = getR(variant)
    StringBuilder content = new StringBuilder()
    project.file(rFilePath).eachLine { line ->
      content.append(line.replace("public static int", "public static final int"))
      content.append('\n')
    }

    project.file(rFilePath).delete()
    project.file(rFilePath) << content.toString()
  }

  String getR(LibraryVariant variant) {
    return "build/generated/source/r/${variant.dirName}/${findPackageName().replace('.', '/')}/R.java"
  }

  String getApt(LibraryVariant variant) {
    return "build/generated/source/apt/${variant.dirName}"
  }

  String getCopiedSource(LibraryVariant variant) {
    return "${project.buildDir.absolutePath}/source/${variant.name}"
  }

  List<File> getAllSourceFiles(String path) {
    List<File> list = new ArrayList<>()
    new File(path).eachFile { File file ->
      if (file.isFile() && file.name.endsWith(".java")) {
        list.add(file)
      }

      if (file.isDirectory()) {
        list.addAll(getAllSourceFiles(file.absolutePath))
      }
    }
    return list
  }

  private String findPackageName() {
    if (packageName) {
      return packageName
    }
    File manifestFile = project.android.sourceSets.main.manifest.srcFile
    return packageName = (new XmlParser()).parse(manifestFile).@package
  }
}
