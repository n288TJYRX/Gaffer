package uk.gov.gchq.gaffer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.SearchItem;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.BuildImageResultCallback;
import org.apache.commons.lang3.exception.CloneFailedException;

import uk.gov.gchq.gaffer.operation.Operation;
import uk.gov.gchq.gaffer.operation.io.InputOutput;
import uk.gov.gchq.gaffer.operation.serialisation.TypeReferenceImpl;

import java.io.File;
import java.util.List;
import java.util.Map;

public class PythonOperation<I_ITEM, O> implements
        InputOutput<Iterable<? extends I_ITEM>, O>,
        Operation {

    private final String operationName;
    private Iterable<? extends I_ITEM> input;
    private Map<String, String> options;
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

//        File directory = new File("./");
//        System.out.println("directory is: " + directory.getAbsolutePath());
//
//        File directory2 = new File("pythonOperation/src/main/resources/Dockerfile");
//        System.out.println("directory is: " + directory2.getAbsolutePath());

        String imageId = dockerClient.buildImageCmd()
                .withDockerfile(new File("pythonOperation/src/main/resources/Dockerfile"))
                .withPull(true)
                .withNoCache(true)
                .withTag("alpine:git")
                .exec(new BuildImageResultCallback())
                .awaitImageId();
        System.out.println("local dockerfile image is: " + imageId);

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

//        CreateContainerResponse container = dockerClient.createContainerCmd(lastCreatedImage.getId())
//                .withExposedPorts(ExposedPort.parse("8080"), ExposedPort.parse("8081"))
////              .withPortSpecs([8080,8081])
//                .exec();


        ExposedPort exposedInputPort = ExposedPort.tcp(8080);
        ExposedPort exposedOutputPort = ExposedPort.tcp(8081);

        Ports portBindings = new Ports();
        portBindings.bind(exposedInputPort, Ports.Binding.bindPort(8082));
        portBindings.bind(exposedOutputPort, Ports.Binding.bindPort(8083));

        CreateContainerResponse container = dockerClient.createContainerCmd(lastCreatedImage.getId())
                .withCmd("true")
                .withExposedPorts(exposedInputPort, exposedOutputPort)
                .withHostConfig(HostConfig.newHostConfig()
                        .withPortBindings(portBindings))
                .exec();

        dockerClient.startContainerCmd(container.getId()).exec();

        System.out.println("created container: " + container);
        String containerId = container.getId();
        System.out.println("created container with id: " + containerId);


        containers = dockerClient.listContainersCmd()
                .withShowSize(true)
                .withShowAll(true)
                .exec();
        System.out.println("list of docker containers: " + containers);

        List<Network> networks = dockerClient.listNetworksCmd().exec();
        System.out.println("list of docker networks: " + networks);
//        dockerClient.stopContainerCmd(container.getId()).exec();

//        dockerClient.startContainerCmd(container.getId()).exec();
//        dockerClient.killContainerCmd(container.getId()).exec();

//        InspectContainerResponse containerInspectionResponse = dockerClient.inspectContainerCmd(container.getId()).exec();
//        System.out.println("container inspection: " + containerInspectionResponse);

//        CreateNetworkResponse networkResponse = dockerClient.createNetworkCmd()
//                .withName("baeldung")
//                .withDriver("bridge").exec();
//        System.out.println("network response: " + networkResponse);
//
//        Network network = dockerClient.inspectNetworkCmd().withNetworkId("baeldung").exec();
//        System.out.println("network: " + network);
//
//        dockerClient.removeNetworkCmd("baeldung").exec();
    }

    @Override
    public Iterable<? extends I_ITEM> getInput() {
        return input;
    }

    @Override
    public void setInput(final Iterable<? extends I_ITEM> input) {
        this.input = input;
    }

    @Override
    public TypeReference<O> getOutputTypeReference() {
        return (TypeReference) new TypeReferenceImpl.Object();
    }

    @Override
    public Operation shallowClone() throws CloneFailedException {
        return null;
    }

    @Override
    public Map<String, String> getOptions() {
        return options;
    }

    @Override
    public void setOptions(final Map<String, String> options) {
        this.options = options;
    }
}

