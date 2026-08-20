package net.blueva.foundation.assets;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

/**
 * Writes files directly into a plugin's own running jar, so Hytale picks
 * them up as bundled resources (asset-pack entries, config defaults,
 * anything else a plugin ships inside itself) the next time the server
 * starts.
 *
 * <p>Hytale has no supported way to register new asset-pack content at
 * runtime that has proven reliable in practice, so this reuses the one
 * mechanism that has: mounting the plugin's own jar as a writable
 * filesystem via {@code java.nio.file} zip support and writing entries
 * into it directly. The change only takes effect after a server restart -
 * this does not attempt to hot-reload anything.</p>
 *
 * <p>This class is intentionally generic: it knows nothing about sounds,
 * items, or any other specific kind of asset. It is a plain "write bytes
 * (or a directory tree) into my own jar" primitive, so any BlueFoundation
 * feature that needs to ship extra files inside a consumer's plugin jar
 * can build on it - see {@code net.blueva.foundation.music} for the one
 * that downloads and injects instrument sounds.</p>
 */
public final class JarAssetInjector {

    private JarAssetInjector() {
    }

    /** Resolves the jar file backing {@code anchor}'s own classloader. */
    public static Path resolveOwnJar(Class<?> anchor) {
        try {
            return Path.of(anchor.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException | NullPointerException e) {
            throw new IllegalStateException("Could not locate the jar backing " + anchor.getName(), e);
        }
    }

    /** @return {@code true} if {@code entryPath} already exists inside {@code anchor}'s jar. */
    public static boolean hasEntry(Class<?> anchor, String entryPath) {
        try (FileSystem jarFs = FileSystems.newFileSystem(resolveOwnJar(anchor), (ClassLoader) null)) {
            return Files.exists(jarFs.getPath(entryPath));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Writes (creating or overwriting) a single entry inside {@code anchor}'s own jar. */
    public static void writeEntry(Class<?> anchor, String entryPath, byte[] content) {
        try (FileSystem jarFs = FileSystems.newFileSystem(resolveOwnJar(anchor), (ClassLoader) null)) {
            Path target = jarFs.getPath(entryPath);
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            Files.write(target, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Copies every regular file under {@code sourceDirectory} into
     * {@code anchor}'s own jar, under {@code entryPrefix}, preserving the
     * relative directory structure. Existing entries at the same path are
     * overwritten.
     */
    public static void writeTree(Class<?> anchor, Path sourceDirectory, String entryPrefix) {
        String prefix = entryPrefix.isEmpty() || entryPrefix.endsWith("/") ? entryPrefix : entryPrefix + "/";
        try (FileSystem jarFs = FileSystems.newFileSystem(resolveOwnJar(anchor), (ClassLoader) null);
             Stream<Path> files = Files.walk(sourceDirectory)) {
            files.filter(Files::isRegularFile).forEach(source -> {
                try {
                    String relative = sourceDirectory.relativize(source).toString().replace('\\', '/');
                    Path target = jarFs.getPath(prefix + relative);
                    if (target.getParent() != null) {
                        Files.createDirectories(target.getParent());
                    }
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
