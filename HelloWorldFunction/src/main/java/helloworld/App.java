package helloworld;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.servicediscovery.AWSServiceDiscovery;
import com.amazonaws.services.servicediscovery.AWSServiceDiscoveryClientBuilder;
import com.amazonaws.services.servicediscovery.model.DiscoverInstancesRequest;
import com.amazonaws.services.servicediscovery.model.DiscoverInstancesResult;
import com.amazonaws.services.servicediscovery.model.HttpInstanceSummary;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<Object, String> {

    public String handleRequest(Object input, Context context) {

        // 1. Create Service Discovery client
        AWSServiceDiscovery discoveryClient = AWSServiceDiscoveryClientBuilder.defaultClient();

        // 2. Prepare discovery request
        DiscoverInstancesRequest request = new DiscoverInstancesRequest()
                .withNamespaceName("demoapp.local") //namespace
                .withServiceName("demoapp.local.hello.api"); //service_name

        // 3. Discover instances
        DiscoverInstancesResult result = discoveryClient.discoverInstances(request);
        List<HttpInstanceSummary> instances = result.getInstances();

        if (instances.isEmpty()) {
            throw new RuntimeException("No instances found for the service.");
        }

        // 4. Choose an instance (you might have a more sophisticated strategy here)
        HttpInstanceSummary instance = instances.get(0);

        // 5. Construct API URL
        String apiUrl = instance.getAttributes().get("AWS_INSTANCE_CNAME"); // Include the path

        // 6. Make the HTTP call
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            throw new RuntimeException("Error calling API: " + e.getMessage(), e);
        }
    }
}
