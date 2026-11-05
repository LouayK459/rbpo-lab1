package com.example.rbpolab1.security;

import com.example.rbpolab1.service.AccountService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class DispatcherBootstrap implements ApplicationRunner {

    private final AccountService accountService;
    private final Environment environment;

    public DispatcherBootstrap(AccountService accountService, Environment environment) {
        this.accountService = accountService;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String username = environment.getProperty("BOOTSTRAP_DISPATCHER_USERNAME", "");
        String password = environment.getProperty("BOOTSTRAP_DISPATCHER_PASSWORD", "");

        if (username.isBlank() && password.isEmpty()) {
            return;
        }

        if (username.isBlank() || password.isEmpty()) {
            throw new IllegalStateException(
                    "Set both BOOTSTRAP_DISPATCHER_USERNAME and BOOTSTRAP_DISPATCHER_PASSWORD");
        }

        String name = environment.getProperty("BOOTSTRAP_DISPATCHER_NAME", "Dispatcher");
        accountService.bootstrapDispatcher(name, username, password);
    }
}