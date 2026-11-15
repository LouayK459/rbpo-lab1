package com.example.rbpolab1;

import com.example.rbpolab1.model.UserSession;
import com.example.rbpolab1.repository.AccountRepository;
import com.example.rbpolab1.repository.UserSessionRepository;
import com.example.rbpolab1.security.JwtTokenProvider;
import com.example.rbpolab1.service.AccountService;
import com.example.rbpolab1.service.TokenPairService;
import com.example.rbpolab1.service.TokenPairService.TokenPair;
import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class Lab5AuthenticationTests {
    private static final String PASSWORD = "Lab5-Test-Password-2026!";
    @Autowired private MockMvc mvc;
    @Autowired private AccountService accounts;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TokenPairService tokens;
    @Autowired private JwtTokenProvider jwt;
    @Autowired private JdbcTemplate sql;
    @MockitoSpyBean private UserSessionRepository sessions;
    @Value("${jwt.secret}") private String secret;
    private String username;
    private AccountService.AccountView account;

    @BeforeEach
    void createAccountWithoutIssuingTokens() {
        username = uniqueUsername();
        account = accounts.register("Lab 5 test " + username, username, PASSWORD);
    }

    @Test
    void loginRotationAndReplayPersistTheRequiredStates() throws Exception {
        TokenPair first = login();
        assertEquals(1, sessionCount());
        assertEquals("ACTIVE", sessionStatus(first));
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + first.accessToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value(username))
                .andExpect(header().doesNotExist("Set-Cookie"));
        assertEquals(jwt.validateAccess(first.accessToken()).getExpiration().toInstant(), expiry(first, "access"));
        assertEquals(jwt.validateRefresh(first.refreshToken()).getExpiration().toInstant(), expiry(first, "refresh"));

        TokenPair second = refresh(first.refreshToken());
        assertNotEquals(first.accessToken(), second.accessToken());
        assertNotEquals(first.refreshToken(), second.refreshToken());
        assertEquals("USED", sessionStatus(first));
        assertEquals("ACTIVE", sessionStatus(second));
        assertEquals("test-device", sql.queryForObject(
                "select device_id from delivery.user_sessions where refresh_token = ?", String.class, second.refreshToken()));

        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + first.refreshToken() + "\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
        assertEquals("REVOKED", sessionStatus(first));
        assertEquals("ACTIVE", sessionStatus(second));
        assertEquals(2, sessionCount());
        // A replay does not revoke the successor or immediately invalidate an access token.
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + first.accessToken()))
                .andExpect(status().isOk());
        assertNotNull(refresh(second.refreshToken()));
    }

    @Test
    void registrationIssuesTokensAndCannotAssignDispatcherRole() throws Exception {
        String registered = uniqueUsername();
        MvcResult result = mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New sender\",\"username\":\"" + registered
                                + "\",\"password\":\"" + PASSWORD + "\",\"role\":\"DISPATCHER\"}"))
                .andExpect(status().isCreated()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.accessToken").isString()).andExpect(jsonPath("$.refreshToken").isString())
                .andReturn();
        TokenPair pair = pair(result);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + pair.accessToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("SENDER"));
        mvc.perform(get("/api/routes").header("Authorization", "Bearer " + pair.accessToken()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New sender\",\"username\":\"" + registered
                                + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    void invalidCredentialsAndMissingFieldsReturnTheSpecifiedStatuses() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.accessToken").doesNotExist());
        for (String body : List.of("{}", "{\"username\":\"sender\"}", "{\"username\":\" \",\"password\":\" \"}")) {
            mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        for (String body : List.of("{}", "{\"refreshToken\":null}", "{\"refreshToken\":\" \"}")) {
            mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        assertEquals(0, sessionCount());
    }

    @Test
    void wrongTokenTypesUnknownExpiredAndTamperedRefreshAreRejected() throws Exception {
        TokenPair pair = login();
        Instant now = Instant.now();
        String expired = Jwts.builder().subject(username).id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now.minusSeconds(120))).expiration(Date.from(now.minusSeconds(60)))
                .claim("type", "refresh").signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)), Jwts.SIG.HS256)
                .compact();
        for (String token : List.of(pair.accessToken(), jwt.createRefresh(username).value(), expired,
                pair.refreshToken().substring(0, pair.refreshToken().lastIndexOf('.') + 1) + "invalid", "not-a-jwt")) {
            mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"" + token + "\"}"))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.accessToken").doesNotExist());
        }
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + pair.refreshToken()))
                .andExpect(status().isUnauthorized());
        assertEquals(1, sessionCount());
        assertEquals("ACTIVE", sessionStatus(pair));
    }

    @Test
    void refreshNeedsNeitherAccessNorPasswordAndIgnoresAnExpiredAccessHeader() throws Exception {
        TokenPair pair = login();
        MvcResult result = mvc.perform(post("/auth/refresh").header("Authorization", "Bearer expired-access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + pair.refreshToken() + "\"}"))
                .andExpect(status().isOk()).andReturn();
        assertNotNull(pair(result).accessToken());
    }

    @Test
    void blockedAndDeletedAccountsCannotUseAccessOrRefresh() throws Exception {
        TokenPair pair = login();
        sql.update("update delivery.accounts set status = 'BLOCKED' where username = ?", username);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + pair.accessToken()))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + pair.refreshToken() + "\"}"))
                .andExpect(status().isUnauthorized());
        sql.update("delete from delivery.accounts where username = ?", username);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + pair.accessToken()))
                .andExpect(status().isUnauthorized());
        assertTrue(tokens.refresh(pair.refreshToken()).isEmpty());
        assertEquals(1, sessionCount());
    }

    @Test
    void sessionOwnerExpiryAndRevokedStateAreCheckedInTheDatabase() {
        TokenPair pair = tokens.issue(username, null);
        sql.update("update delivery.user_sessions set username = 'another-owner' where refresh_token = ?", pair.refreshToken());
        assertTrue(tokens.refresh(pair.refreshToken()).isEmpty());
        sql.update("update delivery.user_sessions set username = ?, refresh_token_expiry = now() - interval '1 second' "
                + "where refresh_token = ?", username, pair.refreshToken());
        assertTrue(tokens.refresh(pair.refreshToken()).isEmpty());
        sql.update("update delivery.user_sessions set refresh_token_expiry = ?, status = 'REVOKED' where refresh_token = ?",
                java.sql.Timestamp.from(jwt.validateRefresh(pair.refreshToken()).getExpiration().toInstant()), pair.refreshToken());
        assertTrue(tokens.refresh(pair.refreshToken()).isEmpty());
        assertEquals("REVOKED", sessionStatus(pair));
        assertEquals(1, sessionCount());
    }

    @Test
    void failedNewSessionSaveRollsBackTheOldUsedStatus() {
        TokenPair pair = tokens.issue(username, null);
        doThrow(new DataIntegrityViolationException("Simulated session insert failure"))
                .when(sessions).saveAndFlush(any(UserSession.class));
        assertThrows(DataIntegrityViolationException.class, () -> tokens.refresh(pair.refreshToken()));
        assertEquals("ACTIVE", sessionStatus(pair));
        assertEquals(1, sessionCount());
    }

    @Test
    void failedRegistrationSessionSaveRollsBackBothAccountAndProfile() {
        String registered = uniqueUsername();
        doThrow(new DataIntegrityViolationException("Simulated session insert failure"))
                .when(sessions).saveAndFlush(any(UserSession.class));
        assertThrows(DataIntegrityViolationException.class,
                () -> applicationRegister(registered));
        assertFalse(accountRepository.existsByUsername(registered));
        assertEquals(0L, sql.queryForObject("select count(*) from delivery.users where name = ?",
                Long.class, "Rollback " + registered));
    }

    @Autowired private com.example.rbpolab1.service.AuthenticationService authenticationService;

    private void applicationRegister(String registered) {
        authenticationService.register("Rollback " + registered, registered, PASSWORD, null);
    }

    @Test
    void simultaneousUseOfOneRefreshHasExactlyOneWinner() throws Exception {
        TokenPair first = tokens.issue(username, "concurrent-device");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Optional<TokenPair>> request = () -> {
            ready.countDown();
            assertTrue(start.await(10, TimeUnit.SECONDS));
            return tokens.refresh(first.refreshToken());
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(request);
            var b = executor.submit(request);
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            List<Optional<TokenPair>> results = List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
            assertEquals(1, results.stream().filter(Optional::isPresent).count());
            TokenPair winner = results.stream().flatMap(Optional::stream).findFirst().orElseThrow();
            assertEquals("REVOKED", sessionStatus(first));
            assertEquals("ACTIVE", sessionStatus(winner));
            assertEquals(2, sessionCount());
        }
    }

    @Test
    void bearerKeepsRoleAndOwnershipRulesWithoutCreatingAnHttpSession() throws Exception {
        TokenPair pair = login();
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/" + account.userId()).header("Authorization", "Bearer " + pair.accessToken()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/users/" + (account.userId() + 1000000)).header("Authorization", "Bearer " + pair.accessToken()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/routes").header("Authorization", "Bearer " + pair.accessToken()))
                .andExpect(status().isForbidden());
    }

    private TokenPair login() throws Exception {
        MvcResult result = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username.toUpperCase(java.util.Locale.ROOT)
                                + "\",\"password\":\"" + PASSWORD + "\",\"deviceId\":\"test-device\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString()).andReturn();
        return pair(result);
    }

    private TokenPair refresh(String token) throws Exception {
        return pair(mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + token + "\"}"))
                .andExpect(status().isOk()).andReturn());
    }

    private TokenPair pair(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString();
        return new TokenPair(JsonPath.read(json, "$.accessToken"), JsonPath.read(json, "$.refreshToken"));
    }

    private long sessionCount() {
        return sql.queryForObject("select count(*) from delivery.user_sessions where username = ?", Long.class, username);
    }

    private String sessionStatus(TokenPair pair) {
        return sql.queryForObject("select status from delivery.user_sessions where refresh_token = ?",
                String.class, pair.refreshToken());
    }

    private Instant expiry(TokenPair pair, String type) {
        return sql.queryForObject("select " + type + "_token_expiry from delivery.user_sessions where refresh_token = ?",
                java.sql.Timestamp.class, pair.refreshToken()).toInstant();
    }

    private String uniqueUsername() {
        return "lab5test-" + UUID.randomUUID();
    }
}
