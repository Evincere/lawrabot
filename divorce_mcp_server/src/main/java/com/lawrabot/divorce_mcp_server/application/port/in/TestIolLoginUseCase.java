package com.lawrabot.divorce_mcp_server.application.port.in;

public interface TestIolLoginUseCase {
    /**
     * Executes a test login against the IOL portal.
     * @return true if login was successful, false otherwise.
     */
    boolean execute();
}
