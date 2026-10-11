import com.sun.source.util.JavacTask;
import javax.tools.*;
import java.nio.file.*;
import java.util.*;
import java.io.*;
import java.util.stream.*;

/** Parse all Java 21 source files without requiring Maven dependencies. */
public class SyntaxAudit {
  public static void main(String[] args) throws Exception {
    if (args.length != 1) throw new IllegalArgumentException("Pass project source root");
    Path root=Paths.get(args[0]);
    List<File> javaFiles;
    try (Stream<Path> files=Files.walk(root)) {
      javaFiles=files.filter(p->p.toString().endsWith(".java"))
        .filter(p->!p.getFileName().toString().equals("SyntaxAudit.java"))
        .map(Path::toFile).toList();
    }
    DiagnosticCollector<JavaFileObject> diagnostics=new DiagnosticCollector<>();
    JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();
    try (StandardJavaFileManager manager=compiler.getStandardFileManager(diagnostics,null,null)) {
      JavacTask task=(JavacTask)compiler.getTask(null,manager,diagnostics,
          List.of("-proc:none","--release","21"),null,
          manager.getJavaFileObjectsFromFiles(javaFiles));
      task.parse();
    }
    List<Diagnostic<? extends JavaFileObject>> failures=diagnostics.getDiagnostics()
        .stream().filter(d->d.getKind()==Diagnostic.Kind.ERROR).toList();
    for(var d:failures)System.err.printf("%s:%d %s%n",d.getSource(),d.getLineNumber(),d.getMessage(Locale.US));
    System.out.printf("Parsed %d Java files; syntax errors: %d%n",javaFiles.size(),failures.size());
    if (!failures.isEmpty()) System.exit(1);
  }
}
