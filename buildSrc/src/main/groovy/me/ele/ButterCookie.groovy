package me.ele

import com.android.build.gradle.api.BaseVariantOutput
import com.android.build.gradle.api.LibraryVariant
import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.visitor.ModifierVisitorAdapter
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import org.apache.commons.io.FileUtils
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.lang.reflect.Field

class ButterCookie implements Plugin<Project> {

  static final String[] ARRAY = ["OnClick",
                                 "OnCheckedChanged",
                                 "OnEditorAction",
                                 "OnFocusChange",
                                 "OnItemClick",
                                 "OnItemLongClick",
                                 "OnItemSelected",
                                 "OnLongClick",
                                 "OnPageChange",
                                 "OnTextChanged",
                                 "OnTouch",
                                 "BindArray",
                                 "BindBitmap",
                                 "BindBool",
                                 "BindColor",
                                 "BindDimen",
                                 "BindDrawable",
                                 "BindInt",
                                 "BindString",
                                 "BindView",
                                 "BindViews"]

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

        project.tasks.findByName("bundle${variant.name.capitalize()}").doLast {
          changeAllFieldsOfIdToFinal(variant)
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

    // apt is no longer needed at this step, so disable it
    variant.javaCompiler.options.compilerArgs.add('-proc:none')
    variant.javaCompiler.state.executed = false
    variant.javaCompiler.execute()
  }

  void deleteAnnotationsFromSource(LibraryVariant variant) {
    String sourcePath = getCopiedSource(variant)
    List<File> files = getAllSourceFiles(sourcePath)
    DeleteAnnotationVisitor visitor = new DeleteAnnotationVisitor()
    files.each { File file ->
      CompilationUnit cu = JavaParser.parse(file, "UTF-8")
      List<TypeDeclaration> types = cu.getTypes()
      types.each { TypeDeclaration type -> deleteAnnotationFromClass(visitor, type)
      }

      file.text = cu.toString()
    }
  }

  void deleteAnnotationFromClass(DeleteAnnotationVisitor visitor, TypeDeclaration type) {
    List<BodyDeclaration> members = type.getMembers()
    members.each { BodyDeclaration member ->
      if (member instanceof MethodDeclaration) {
        visitor.visit((MethodDeclaration) member, null)
      } else if (member instanceof FieldDeclaration) {
        visitor.visit((FieldDeclaration) member, null)
      } else if (member instanceof TypeDeclaration) {
        deleteAnnotationFromClass(visitor, (TypeDeclaration) member)
      }
    }
  }

  void replaceIdsInViewBinders(LibraryVariant variant) {
    String aptDirPath = "${project.buildDir.absolutePath}/generated/source/apt/${variant.dirName}"
    List<File> viewBinderFiles = getAllViewBinders(aptDirPath)
    Map<String, String> mapping = getMappings(variant)
    viewBinderFiles.each { File file ->
      file.text = new IdReplacementVisitor(mapping).visit(JavaParser.parse(file, "UTF-8"), null).toString()
    }
  }

  Map<String, String> getMappings(LibraryVariant variant) {
    Map<String, String> map = new HashMap<>()
    CompilationUnit cu = JavaParser.parse(project.file(getR(variant)), "UTF-8")
    List<TypeDeclaration> types = cu.getTypes()
    types.each { TypeDeclaration type ->
      List<BodyDeclaration> members = type.getMembers()
      members.each { BodyDeclaration member ->
        if (member instanceof TypeDeclaration) {
          new CollectMapVisitor(((TypeDeclaration) member).name, findPackageName(), map).visit(
              member, null)
        }
      }
    }

    return map
  }

  void deleteAllFinalInIds(LibraryVariant variant) {
    File rFile = project.file(getR(variant))
    rFile.text =
        new ChangeIdFinalVisitor(false).visit(JavaParser.parse(rFile, "UTF-8"), null).toString()
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

  //change all fields in R.class to final
  void changeAllFieldsOfIdToFinal(LibraryVariant variant) {
    File rFile = project.file(getR(variant))
    rFile.text =
        new ChangeIdFinalVisitor(true).visit(JavaParser.parse(rFile, "UTF-8"), null).toString()
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

  public static class ChangeIdFinalVisitor extends ModifierVisitorAdapter {

    boolean tobeFinal

    ChangeIdFinalVisitor(boolean tobeFinal) {
      this.tobeFinal = tobeFinal
    }

    @Override
    Node visit(FieldDeclaration n, Object arg) {
      if (tobeFinal) {
        n.setModifiers(n.modifiers | ModifierSet.FINAL)
      } else {
        n.setModifiers(n.modifiers & ~ModifierSet.FINAL)
      }
      return n
    }
  }

  public static class IdReplacementVisitor extends ModifierVisitorAdapter {
    private Map<String, String> map;

    IdReplacementVisitor(Map<String, String> map) {
      this.map = map
    }

    @Override
    Node visit(MethodCallExpr n, Object a) {
      List<Expression> newArgs = new ArrayList<>()
      List<Expression> args = n.args
      args?.each { Expression arg ->
        if (arg instanceof IntegerLiteralExpr) {
          int value = Integer.parseInt(arg.value)
          String key = findMappedId(value)
          if (key != null) {
            newArgs.add(new NameExpr(key))
            return
          }
        }
        newArgs.add(arg)
      }
      n.setArgs(newArgs)
      return n
    }

    String findMappedId(int value) {
      for (Map.Entry<String, String> entry : map.entrySet()) {
        int v = Integer.parseInt(entry.key)
        if (v == value) {
          return entry.value
        }
      }
      return null
    }
  }

  public static class CollectMapVisitor extends VoidVisitorAdapter {

    String clsName
    String pkgName;
    Map<String, String> map

    CollectMapVisitor(String clsName, String pkgName, Map<String, String> map) {
      this.clsName = clsName
      this.pkgName = pkgName
      this.map = map
    }

    @Override
    void visit(FieldDeclaration n, Object arg) {
      if (n.variables.size() == 1 && n.type.toString() == 'int') {
        VariableDeclarator declarator = n.variables.get(0)
        map.put(Integer.decode(declarator.init.toStringWithoutComments()).toString(),
            "${pkgName}.R.${clsName}.${declarator.id.name}")
      }
    }
  }

  public static class DeleteAnnotationVisitor extends ModifierVisitorAdapter {
    @Override
    Node visit(MethodDeclaration n, Object arg) {
      return n.setAnnotations(filterAnnotations(n.getAnnotations()))
    }

    @Override
    Node visit(FieldDeclaration n, Object arg) {
      return n.setAnnotations(filterAnnotations(n.getAnnotations()))
    }

    List<AnnotationExpr> filterAnnotations(List<AnnotationExpr> annotations) {
      if (annotations == null || annotations.size() == 0) {
        return annotations
      }
      annotations.each { AnnotationExpr annotation ->
        if (ARRAY.contains(annotation.name.toString())) {
          annotations.remove(annotation)
        }
      }
      return annotations
    }
  }
}
