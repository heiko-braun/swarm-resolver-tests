package info.hbraun.resolver.shrinkwrap;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import info.hbraun.resolver.Demo;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.jboss.shrinkwrap.resolver.api.maven.ConfigurableMavenResolverSystem;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolvedArtifact;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenCoordinate;
import org.jboss.shrinkwrap.resolver.api.maven.repository.MavenChecksumPolicy;
import org.jboss.shrinkwrap.resolver.api.maven.repository.MavenRemoteRepositories;
import org.jboss.shrinkwrap.resolver.api.maven.repository.MavenRemoteRepository;
import org.jboss.shrinkwrap.resolver.api.maven.repository.MavenUpdatePolicy;
import org.jboss.shrinkwrap.resolver.impl.maven.ConfigurableMavenWorkingSessionImpl;
import org.jboss.shrinkwrap.resolver.impl.maven.MavenWorkingSessionContainer;
import org.wildfly.swarm.arquillian.resolver.FailureReportingTransferListener;
import org.wildfly.swarm.arquillian.resolver.ShrinkwrapArtifactResolvingHelper;
import org.wildfly.swarm.spi.api.internal.SwarmInternalProperties;
import org.wildfly.swarm.tools.ArtifactSpec;

/**
 * @author Heiko Braun
 * @since 06/12/2016
 */
public class ShrinkwrapDemo implements Demo {


    public final static ConfigurableMavenResolverSystem RESOLVER_INSTANCE = createResolver();

    @Override
    public void execute(File pomFile) throws Exception {

        ConfigurableMavenResolverSystem resolver = RESOLVER_INSTANCE;

        DefaultRepositorySystemSession session = ShrinkwrapDemo.session(resolver);
        session.setTransferListener(new FailureReportingTransferListener());

        ShrinkwrapArtifactResolvingHelper resolvingHelper = new ShrinkwrapArtifactResolvingHelper(resolver);
        PomEquippedResolveStage pomEquipped = loadPom(resolver, pomFile);

        /*
        MavenWorkingSession session = ((MavenWorkingSessionContainer) resolver).getMavenWorkingSession();
                        Set<MavenDependency> explicitDeps = session.getDeclaredDependencies()
                                .stream()
                                .filter(dep -> (
                                        dep.getScope().equals(ScopeType.RUNTIME)
                                        || dep.getScope().equals(ScopeType.COMPILE)
                                        || dep.getScope().equals(ScopeType.TEST))
                                )
                                .collect(Collectors.toSet());
*/
        // NonTransitiveStrategy
        final MavenResolvedArtifact[] explicitDeps =
                resolvingHelper.withResolver(r -> pomEquipped
                        .importRuntimeAndTestDependencies()
                        .resolve()
                        .withoutTransitivity()
                        .asResolvedArtifact()
                );

        for (MavenResolvedArtifact dep : explicitDeps) {
            MavenCoordinate coord = dep.getCoordinate();
            ArtifactSpec artifactSpec = new ArtifactSpec(
                    dep.getScope().name(), coord.getGroupId(),
                    coord.getArtifactId(), coord.getVersion(),
                    coord.getPackaging().getExtension(), coord.getClassifier(), dep.asFile()
            );

        }

        // TransitiveStrategy

        final MavenResolvedArtifact[] transientDeps =
                resolvingHelper.withResolver(r -> pomEquipped
                        .importRuntimeAndTestDependencies()
                        .resolve()
                        .withTransitivity()
                        .asResolvedArtifact());

        for (MavenResolvedArtifact dep : transientDeps) {
            MavenCoordinate coord = dep.getCoordinate();
            ArtifactSpec artifactSpec = new ArtifactSpec(
                    dep.getScope().name(), coord.getGroupId(),
                    coord.getArtifactId(), coord.getVersion(),
                    coord.getPackaging().getExtension(), coord.getClassifier(), dep.asFile()
            );

        }

        System.out.println(getClass().getSimpleName());
        System.out.println("Direct: "+explicitDeps.length);
        System.out.println("Transitive: "+transientDeps.length);
        System.out.println();
    }

    public static PomEquippedResolveStage loadPom(ConfigurableMavenResolverSystem resolver, File pom) {
        return resolver.loadPomFromFile(pom);
    }

    public static ConfigurableMavenResolverSystem createResolver() {
        MavenRemoteRepository jbossPublic =
                MavenRemoteRepositories.createRemoteRepository("jboss-public-repository-group",
                                                               "http://repository.jboss.org/nexus/content/groups/public/",
                                                               "default");
        jbossPublic.setChecksumPolicy(MavenChecksumPolicy.CHECKSUM_POLICY_IGNORE);
        jbossPublic.setUpdatePolicy(MavenUpdatePolicy.UPDATE_POLICY_NEVER);


        MavenRemoteRepository gradleTools =
                MavenRemoteRepositories.createRemoteRepository("gradle",
                                                               "http://repo.gradle.org/gradle/libs-releases-local",
                                                               "default");
        gradleTools.setChecksumPolicy(MavenChecksumPolicy.CHECKSUM_POLICY_IGNORE);
        gradleTools.setUpdatePolicy(MavenUpdatePolicy.UPDATE_POLICY_NEVER);


        String localM2 = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository/";
        MavenRemoteRepository local =
                MavenRemoteRepositories.createRemoteRepository("local",
                                                               "file://"+localM2,
                                                               "default");
        local.setChecksumPolicy(MavenChecksumPolicy.CHECKSUM_POLICY_IGNORE);
        local.setUpdatePolicy(MavenUpdatePolicy.UPDATE_POLICY_ALWAYS);

        Boolean offline = Boolean.valueOf(System.getProperty("swarm.resolver.offline", "false"));
        final ConfigurableMavenResolverSystem resolver = Maven.configureResolver()
                .withMavenCentralRepo(true)
                //.withRemoteRepo(local)
                .withRemoteRepo(jbossPublic)
                .withRemoteRepo(gradleTools)
                .workOffline(offline);

        final String additionalRepos = System.getProperty(SwarmInternalProperties.BUILD_REPOS);
        if (additionalRepos != null) {
            Arrays.asList(additionalRepos.split(","))
                    .forEach(r -> {
                        MavenRemoteRepository repo =
                                MavenRemoteRepositories.createRemoteRepository(r, r, "default");
                        repo.setChecksumPolicy(MavenChecksumPolicy.CHECKSUM_POLICY_IGNORE);
                        repo.setUpdatePolicy(MavenUpdatePolicy.UPDATE_POLICY_NEVER);
                        resolver.withRemoteRepo(repo);
                    });
        }

        session(resolver).setCache(new DefaultRepositoryCache());

        return resolver;
    }

    public static DefaultRepositorySystemSession session(ConfigurableMavenResolverSystem resolver) {
        return (DefaultRepositorySystemSession) invokeWorkingSessionMethod(resolver, "getSession");
    }

    private static Object invokeWorkingSessionMethod(ConfigurableMavenResolverSystem resolver, final String methodName) {
        try {
            final Method method = ConfigurableMavenWorkingSessionImpl.class.getDeclaredMethod(methodName);
            method.setAccessible(true);

            return method.invoke(((MavenWorkingSessionContainer) resolver).getMavenWorkingSession());
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Failed to invoke " + methodName, e);
        }
    }

}
