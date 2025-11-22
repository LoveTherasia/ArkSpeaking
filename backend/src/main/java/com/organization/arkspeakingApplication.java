package com.organization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class arkspeakingApplication
{
    public static void main( String[] args )
    {
        int basePort = getBasePort();
        int selectedPort = findAvailablePort(basePort);

        SpringApplication app = new SpringApplication(arkspeakingApplication.class);
        Map<String, Object> props = new HashMap<>();
        props.put("server.port", selectedPort);
        app.setDefaultProperties(props);
        app.run(args);
    }

    private static int getBasePort() {
        String envPort = System.getenv("APP_PORT");
        if (envPort != null) {
            try {
                return Integer.parseInt(envPort);
            } catch (NumberFormatException ignored) {}
        }
        String ymlPort = System.getProperty("server.port");
        if (ymlPort != null) {
            try {
                return Integer.parseInt(ymlPort);
            } catch (NumberFormatException ignored) {}
        }
        return 8081;
    }

    private static int findAvailablePort(int preferred) {
        for (int port = preferred; port < preferred + 50; port++) {
            if (isPortFree(port)) {
                return port;
            }
        }
        return 0;
    }

    private static boolean isPortFree(int port) {
        try (ServerSocket socket = new ServerSocket(port, 1, InetAddress.getByName("0.0.0.0"))) {
            socket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
