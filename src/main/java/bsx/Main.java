package bsx;

import bsx.compiler.CompiledProgram;
import bsx.compiler.CompilerAPI;
import bsx.compiler.ast.Program;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.util.PathConverter;
import joptsimple.util.PathProperties;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class Main {
    
    public static void main(String[] args) throws Throwable {
        OptionParser options = new OptionParser(false);
        
        OptionSpec<Void> specDebug = options.acceptsAll(List.of("d", "debug"), "Run in debug mode.");
        OptionSpec<Path> specDump = options.acceptsAll(List.of("c", "dump"), "Dump generated .class files to given folder while running.").withRequiredArg().withValuesConvertedBy(new PathConverter());
        OptionSpec<Path> specLibrary = options.acceptsAll(List.of("l", "lib", "library"), "Additional bs files to load. They are loaded in order and their main code is not executed.").withRequiredArg().withValuesConvertedBy(new PathConverter(PathProperties.FILE_EXISTING)).withValuesSeparatedBy(File.pathSeparator);
        OptionSpec<Path> specMainFile = options.nonOptions("The bs file to run").withValuesConvertedBy(new PathConverter(PathProperties.FILE_EXISTING));

        OptionSet set;
        try {
            set = options.parse(args);
            if (!set.has(specMainFile) || set.valuesOf(specMainFile).size() == 0) {
                System.err.println("No input file.");
                options.printHelpOn(System.err);
                System.exit(1);
                throw new Error();
            } else if (set.valuesOf(specMainFile).size() > 1) {
                System.err.println("More than one input file. Use -lib to load libraries.");
                options.printHelpOn(System.err);
                System.exit(1);
                throw new Error();
            }
        } catch (OptionException e) {
            System.err.println(e.getMessage());
            options.printHelpOn(System.err);
            System.exit(1);
            throw new Error();
        }
        
        Bootstrap.bootstrap(set.has(specDebug));
        Path dumpPath = null;
        if (set.has(specDump)) {
            dumpPath = set.valueOf(specDump).toAbsolutePath().normalize();
            if (!Files.exists(dumpPath) && dumpPath.getParent() != null && Files.isDirectory(dumpPath.getParent())) {
                Files.createDirectories(dumpPath);
            }
            if (!Files.isDirectory(dumpPath)) {
                throw new IllegalArgumentException("Dump path not found: " + dumpPath);
            }
            Bootstrap.context().dumpTo(dumpPath);
        }

        CompilerAPI api = CompilerAPI.get();
        
        for (Path lib : set.valuesOf(specLibrary)) {
            CompiledProgram program = compile(api, lib, dumpPath);
            program.loadIntoCurrentEnvironment();
        }
        
        CompiledProgram mainProgram = compile(api, set.valueOf(specMainFile), dumpPath);
        MethodHandle main = mainProgram.loadIntoCurrentEnvironment();
        
        BSX.invokeWithErrorHandling(main);
    }
    
    private static CompiledProgram compile(CompilerAPI api, Path path, @Nullable Path dumpPath) throws IOException {
        String program = Files.readString(path.toAbsolutePath().normalize());
        String preprocessed = api.preprocess(program);
        if (dumpPath != null) {
            String fileName = path.toAbsolutePath().normalize().getFileName().toString();
            if (fileName.endsWith(".bs")) fileName += "x";
            Path target = dumpPath.resolve(fileName);
            Files.writeString(target, preprocessed, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            fileName = fileName.endsWith(".bsx") ? fileName.substring(0, fileName.length() - 4) : fileName;
            fileName += ".tk";
            Path tokenTarget = dumpPath.resolve(fileName);
            String tokenized = api.tokenize(preprocessed);
            Files.writeString(tokenTarget, tokenized, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        Program ast = api.parseAST(preprocessed);
        return api.compile(path.toAbsolutePath().normalize().getFileName().toString(), ast, null);
    }
}
