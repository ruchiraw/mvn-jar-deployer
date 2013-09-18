package org.wso2.carbon.mvnjardeployer;

import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallationException;
import org.eclipse.aether.util.artifact.SubArtifact;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Hello world!
 */
public class Client {

    private static Logger log = Logger.getLogger(Client.class);

    private static RepositorySystem system;
    private static RepositorySystemSession session;

    private static CommandLineParser parser = new BasicParser();
    private static Options options = new Options();

    public static void main(String[] args) throws Exception {
        initOptions();

        CommandLine line = parser.parse(options, args);

        if (line.hasOption("help") || !line.hasOption("repo") || !line.hasOption("path")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("ant", options);
            return;
        }

        String repo = line.getOptionValue("repo");
        String path = line.getOptionValue("path");

        system = Booter.newRepositorySystem();
        session = Booter.newRepositorySystemSession(repo, system);

        File file = new File(path);
        read(file);
    }

    private static void read(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) {
                return;
            }
            for (File f : files) {
                read(f);
            }
            return;
        }
        if (!file.getName().endsWith(".jar")) {
            return;
        }

        try {
            process(file);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private static void process(File file) throws IOException, InstallationException {
        System.out.println("processing jar file : " + file.getAbsolutePath());
        JarFile jar = new JarFile(file);
        Enumeration entries = jar.entries();
        Properties properties = null;
        File pom = null;
        while (entries.hasMoreElements()) {
            JarEntry entry = (JarEntry) entries.nextElement();
            String name = entry.getName();
            if (!name.endsWith("/pom.properties") && !name.endsWith("/pom.xml")) {
                continue;
            }
            if (name.endsWith("/pom.properties")) {
                properties = loadProperties(jar.getInputStream(entry));
            }
            if (name.endsWith("/pom.xml")) {
                pom = new File("pom.xml");
                IOUtils.copy(jar.getInputStream(entry), new FileOutputStream(pom));
            }
            if (properties == null || pom == null) {
                continue;
            }
            install(file, pom, properties);
            break;
        }
    }

    private static void initOptions() {
        options.addOption("help", false, "prints this message");
        options.addOption("repo", true, "m2 repository location e.g. /home/ruchira/.m2/repository");
        options.addOption("path", true, "path to search for jars e.g. /home/ruchira/lib");
    }

    private static Properties loadProperties(InputStream is) throws IOException {
        Properties properties = new Properties();
        properties.load(is);
        return properties;
    }

    private static void install(File jar, File pom, Properties properties) throws InstallationException {
        String groupId = properties.getProperty("groupId");
        String artifactId = properties.getProperty("artifactId");
        String version = properties.getProperty("version");

        Artifact jarArtifact = new DefaultArtifact(groupId, artifactId, "", "jar", version);
        jarArtifact = jarArtifact.setFile(jar);

        Artifact pomArtifact = new SubArtifact(jarArtifact, "", "pom");
        pomArtifact = pomArtifact.setFile(pom);

        InstallRequest installRequest = new InstallRequest();
        installRequest.addArtifact(jarArtifact).addArtifact(pomArtifact);

        system.install(session, installRequest);
    }
}
