/*
 * Copyright 2021 - 2026 the TODAY authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.cloud.plugin;

import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import infra.bytecode.AnnotationVisitor;
import infra.bytecode.ClassReader;
import infra.bytecode.ClassVisitor;
import infra.bytecode.Type;
import infra.lang.Constant;

/**
 * Utility class for finding service interfaces within a given directory structure by scanning
 * compiled {@code .class} files. This class traverses the directory tree, reads class bytecode
 * to identify public interfaces, and optionally filters them based on specific annotations.
 * <p>
 * The traversal order is deterministic: directories and files are processed in alphabetical order
 * to ensure consistent results across runs.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class ServiceClassFinder {

  private static final String DOT_CLASS = ".class";

  private static final String SERVICE = "infra.stereotype.Service";

  /**
   * File filter that accepts only files ending with {@code .class}.
   */
  private static final FileFilter CLASS_FILE_FILTER = ServiceClassFinder::isClassFile;

  /**
   * File filter that accepts only directories that do not start with a dot (hidden directories).
   */
  private static final FileFilter PACKAGE_DIRECTORY_FILTER = ServiceClassFinder::isPackageDirectory;

  /**
   * Checks if the given file is a valid Java class file.
   *
   * @param file the file to check
   * @return {@code true} if the file is a regular file ending with {@code .class}, {@code false} otherwise
   */
  private static boolean isClassFile(File file) {
    return file.isFile() && file.getName().endsWith(DOT_CLASS);
  }

  /**
   * Checks if the given file is a valid package directory.
   * <p>
   * A valid package directory is a directory that does not start with a dot (i.e., not hidden).
   *
   * @param file the file to check
   * @return {@code true} if the file is a directory and not hidden, {@code false} otherwise
   */
  private static boolean isPackageDirectory(File file) {
    return file.isDirectory() && !file.getName().startsWith(".");
  }

  /**
   * Finds all public interface names within the specified root directory that are annotated
   * with the given annotation name.
   *
   * @param rootDirectory the root directory to start scanning from
   * @param annotationName the fully qualified name of the annotation to filter by, or {@code null} to ignore annotation filtering
   * @return a list of fully qualified interface names matching the criteria
   * @throws IOException if an I/O error occurs while reading class files
   */
  public static List<String> findInterfaces(File rootDirectory, @Nullable String annotationName) throws IOException {
    InterfacesCallback callback = new InterfacesCallback(annotationName);
    ServiceClassFinder.doWithInterface(rootDirectory, callback);
    return callback.getInterfaces();
  }

  /**
   * Finds all public interface names within the specified root directory that are annotated
   * with the given annotation name.
   *
   * @param rootDirectory the root directory to start scanning from
   * @throws IOException if an I/O error occurs while reading class files
   */
  public static List<String> findInterfaces(File rootDirectory) throws IOException {
    return findInterfaces(rootDirectory, SERVICE);
  }

  /**
   * Traverses the directory structure starting from {@code rootDirectory}, reads each {@code .class} file,
   * and invokes the provided {@code callback} for every valid public interface found.
   * <p>
   * If the callback returns a non-null result, the traversal stops immediately and the result is returned.
   *
   * @param rootDirectory the root directory to start scanning from
   * @param callback the callback to invoke for each discovered interface
   * @param <T> the return type of the callback
   * @return the first non-null result returned by the callback, or {@code null} if no such result is produced
   * @throws IOException if an I/O error occurs or if {@code rootDirectory} is not a valid directory
   */
  static <T> @Nullable T doWithInterface(File rootDirectory, InterfaceCallback<T> callback) throws IOException {
    if (!rootDirectory.exists()) {
      return null; // nothing to do
    }
    if (!rootDirectory.isDirectory()) {
      throw new IllegalArgumentException("Invalid root directory '" + rootDirectory + "'");
    }
    Deque<File> stack = new ArrayDeque<>();
    stack.push(rootDirectory);
    while (!stack.isEmpty()) {
      File file = stack.pop();
      if (file.isFile()) {
        try (InputStream inputStream = new FileInputStream(file)) {
          ClassDescriptor classDescriptor = createClassDescriptor(inputStream);
          if (classDescriptor != null && classDescriptor.name != null) {
            T result = callback.doWith(new InterfaceClass(classDescriptor.name, classDescriptor.annotationNames));
            if (result != null) {
              return result;
            }
          }
        }
      }
      if (file.isDirectory()) {
        pushAllSorted(stack, file.listFiles(PACKAGE_DIRECTORY_FILTER));
        pushAllSorted(stack, file.listFiles(CLASS_FILE_FILTER));
      }
    }
    return null;
  }

  /**
   * Pushes all files from the given array onto the stack in sorted order (by name).
   * <p>
   * Sorting ensures a deterministic traversal order.
   *
   * @param stack the stack to push files onto
   * @param files the array of files to sort and push; may be {@code null}
   */
  private static void pushAllSorted(Deque<File> stack, File @Nullable [] files) {
    if (files != null) {
      Arrays.sort(files, Comparator.comparing(File::getName));
      for (File file : files) {
        stack.push(file);
      }
    }
  }

  /**
   * Reads the input stream containing bytecode and extracts class metadata including the class name
   * and associated annotation names.
   *
   * @param inputStream the input stream containing the class file data
   * @return a {@link ClassDescriptor} containing the extracted metadata, or {@code null} if parsing fails
   */
  private static @Nullable ClassDescriptor createClassDescriptor(InputStream inputStream) {
    try {
      ClassReader classReader = new ClassReader(inputStream);
      ClassDescriptor classDescriptor = new ClassDescriptor();
      classReader.accept(classDescriptor, ClassReader.SKIP_CODE);
      return classDescriptor;
    }
    catch (IOException ex) {
      return null;
    }
  }

  /**
   * A {@link ClassVisitor} implementation that captures the name of a public interface
   * and collects the names of all annotations present on the class.
   */
  private static final class ClassDescriptor extends ClassVisitor {

    /**
     * The set of fully qualified annotation names found on the class.
     */
    public final Set<String> annotationNames = new LinkedHashSet<>();

    /**
     * The internal name of the class if it is a public interface; otherwise {@code null}.
     */
    public @Nullable String name;

    @Override
    public @Nullable AnnotationVisitor visitAnnotation(String desc, boolean visible) {
      this.annotationNames.add(Type.forDescriptor(desc).getClassName());
      return null;
    }

    @Override
    public void visit(int version, int access, String name, @Nullable String signature, @Nullable String superName, String @Nullable [] interfaces) {
      if (Modifier.isPublic(access) && Modifier.isInterface(access)) {
        this.name = name.replace(Constant.PATH_SEPARATOR, Constant.PACKAGE_SEPARATOR);
      }
    }

  }

  /**
   * A callback interface used during the traversal of class files.
   *
   * @param <T> the return type of the {@link #doWith(InterfaceClass)} method
   */
  interface InterfaceCallback<T> {

    /**
     * Invoked for each discovered public interface.
     *
     * @param interfaceClass the descriptor of the discovered interface
     * @return a result value, or {@code null} to continue traversal
     */
    @Nullable
    T doWith(InterfaceClass interfaceClass);

  }

  /**
   * Represents a discovered public interface along with its associated annotations.
   */
  static final class InterfaceClass {

    private final String name;

    private final Set<String> annotationNames;

    InterfaceClass(String name, Set<String> annotationNames) {
      this.name = name;
      this.annotationNames = Set.copyOf(annotationNames);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      InterfaceClass other = (InterfaceClass) obj;
      return this.name.equals(other.name);
    }

    @Override
    public int hashCode() {
      return this.name.hashCode();
    }

    @Override
    public String toString() {
      return this.name;
    }

  }

  /**
   * A callback implementation that collects interface names matching a specific annotation.
   */
  private static final class InterfacesCallback implements InterfaceCallback<Object> {

    private final Set<String> interfaces = new LinkedHashSet<>();

    private final @Nullable String annotationName;

    /**
     * Constructs a new callback with the optional annotation filter.
     *
     * @param annotationName the annotation name to filter by, or {@code null} to accept all interfaces
     */
    private InterfacesCallback(@Nullable String annotationName) {
      this.annotationName = annotationName;
    }

    @Override
    public @Nullable Object doWith(InterfaceClass interfaceClass) {
      if (interfaceClass.annotationNames.contains(annotationName)) {
        interfaces.add(interfaceClass.name);
      }
      return null;
    }

    /**
     * Returns the collected list of interface names.
     *
     * @return a list of interface names that matched the annotation filter
     */
    public List<String> getInterfaces() {
      return new ArrayList<>(interfaces);
    }

  }

}
