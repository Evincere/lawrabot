package com.lawrabot.divorce_mcp_server.application.service;

import com.lawrabot.divorce_mcp_server.application.port.in.TestIolLoginUseCase;
import com.lawrabot.divorce_mcp_server.infrastructure.iol.PlaywrightIolClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestIolLoginService implements TestIolLoginUseCase {

    private final PlaywrightIolClient playwrightIolClient;

    @Override
    public boolean execute() {
        log.info("TestIolLoginService: delegando al PlaywrightIolClient para intentar login en IOL.");
        return playwrightIolClient.testLogin();
    }
}
