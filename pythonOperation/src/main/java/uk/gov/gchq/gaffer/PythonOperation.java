package uk.gov.gchq.gaffer;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PythonOperation {

    private final String operationName;
//    private static final Logger LOGGER = LoggerFactory.getLogger(PythonOperation.class);

    public PythonOperation(final String name) {
        this.operationName = name;
    }

    public static void main(String[] args) {

//        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
//                .withDockerHost("tcp://localhost:8080")
////                .withDockerTlsVerify(true) // Needed to communicate over a network
////                .withDockerCertPath("/home/user/.docker/certs")
//                .withDockerConfig("/home/user/.docker")
//                .withApiVersion("1.30") // optional
//                .withRegistryUrl("https://index.docker.io/v1/")
//                .withRegistryUsername("dockeruser")
//                .withRegistryPassword("ilovedocker")
//                .withRegistryEmail("dockeruser@github.com")
//                .build();
//        DockerClient docker = DockerClientBuilder.getInstance().build();

        DefaultDockerClientConfig.Builder config = DefaultDockerClientConfig.createDefaultConfigBuilder();
        DockerClient dockerClient = DockerClientBuilder
                .getInstance(config)
                .build();

//        // using jaxrs/jersey implementation here (netty impl is also available)
//        DockerCmdExecFactory dockerCmdExecFactory = new JerseyDockerCmdExecFactory()
//                .withReadTimeout(1000)
//                .withConnectTimeout(1000)
//                .withMaxTotalConnections(100)
//                .withMaxPerRouteConnections(10);

//        DockerClient dockerClient = DockerClientBuilder.getInstance(config)
//                .withDockerCmdExecFactory(dockerCmdExecFactory)
//                .build();

        Info info = dockerClient.infoCmd()
                .exec();
        System.out.println("docker info: " + info);

        List<SearchItem> dockerSearch = dockerClient.searchImagesCmd("busybox")
                .exec();
        System.out.println("Search for busybox returned: " + dockerSearch.toString());
//        LOGGER.info("Searching for image: ", dockerSearch.toString());

        List<Container> containers = dockerClient.listContainersCmd()
                .withShowSize(true)
                .withShowAll(true)
                .exec();
        System.out.println("list of docker containers: " + containers);

        Image lastCreatedImage = dockerClient.listImagesCmd().exec().get(0);
        System.out.println("image 0: " + lastCreatedImage);

        String repository = lastCreatedImage.getRepoTags()[0];
        System.out.println("repository: " + repository);

//        CreateContainerResponse container
//                = dockerClient.createContainerCmd("mongo:3.6")
//                .withCmd("--bind_ip_all")
//                .withName("mongo")
//                .withHostName("baeldung")
//                .withEnv("MONGO_LATEST_VERSION=3.6")
//                .withPortBindings(PortBinding.parse("9999:27017"))
//                .withBinds(Bind.parse("/Users/baeldung/mongo/data/db:/data/db")).exec();

        System.out.println("image id: " + lastCreatedImage.getId());

        String containerId = dockerClient.createContainerCmd(lastCreatedImage.getId()).exec().getId();
        System.out.println("created container with id: " + containerId);
    }
}

