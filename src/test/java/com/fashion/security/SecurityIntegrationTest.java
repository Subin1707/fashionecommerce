package com.fashion.security;

import com.fashion.auth.dto.RegisterRequest;
import com.fashion.auth.service.AuthService;
import com.fashion.order.entity.Order;
import com.fashion.order.repository.OrderRepository;
import com.fashion.user.entity.Role;
import com.fashion.user.entity.User;
import com.fashion.user.repository.RoleRepository;
import com.fashion.user.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class SecurityIntegrationTest {

    private static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Value("${fashion.jwt.secret}")
    private String jwtSecret;

    private Role customerRole;
    private String customerToken;
    private User customerA;
    private User customerB;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        orderRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        customerRole = roleRepository.save(new Role("CUSTOMER"));
        roleRepository.save(new Role("ADMIN"));

        customerA = userRepository.save(user("customer-a-sec@example.com", customerRole));
        customerB = userRepository.save(user("customer-b-sec@example.com", customerRole));
        customerToken = jwtService.generateToken(customerA.getEmail(), "CUSTOMER");
    }

    @Test
    void sec02PasswordIsStoredAsBCryptHash() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Security Password");
        request.setEmail("security-password@example.com");
        request.setPassword("PlainPass@123");
        request.setPhone("0909000001");
        request.setAddress("Hanoi");

        authService.register(request);

        User storedUser = userRepository.findByEmail("security-password@example.com").orElseThrow();
        assertThat(storedUser.getPassword()).isNotEqualTo("PlainPass@123");
        assertThat(storedUser.getPassword()).startsWith("$2");
        assertThat(passwordEncoder.matches("PlainPass@123", storedUser.getPassword())).isTrue();
    }

    @Test
    void sec05AddsContentSecurityPolicyHeader() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(header().string(CONTENT_SECURITY_POLICY, containsString("default-src 'self'")))
                .andExpect(header().string(CONTENT_SECURITY_POLICY, containsString("frame-ancestors 'none'")));
    }

    @Test
    void sec06CsrfUsesStatelessJwtBearerTokenInsteadOfSynchronizerToken() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken))
                        .contentType("application/json")
                        .content("""
                                {
                                  "shippingAddress": "Hanoi",
                                  "paymentMethod": "COD"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sec07CustomerTokenCannotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void sec08CustomerCannotReadAnotherCustomersOrder() throws Exception {
        Order otherCustomersOrder = orderRepository.save(Order.builder()
                .userId(customerB.getId())
                .totalAmount(100000.0)
                .shippingFee(30000.0)
                .discountAmount(0.0)
                .finalAmount(130000.0)
                .shippingAddress("Ho Chi Minh City")
                .paymentMethod("COD")
                .paymentStatus("PENDING")
                .orderStatus("PENDING")
                .build());

        mockMvc.perform(get("/api/orders/{orderId}", otherCustomersOrder.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sec09InvalidJwtCannotAccessProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer("invalid.jwt.token")))
                .andExpect(status().isForbidden());
    }

    @Test
    void sec10ExpiredJwtCannotAccessProtectedEndpoint() throws Exception {
        String expiredToken = Jwts.builder()
                .setSubject(customerA.getEmail())
                .claim("role", "CUSTOMER")
                .setIssuedAt(new Date(System.currentTimeMillis() - 7_200_000))
                .setExpiration(new Date(System.currentTimeMillis() - 3_600_000))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        mockMvc.perform(get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer(expiredToken)))
                .andExpect(status().isForbidden());
    }

    private User user(String email, Role role) {
        return User.builder()
                .fullName("Security User")
                .email(email)
                .password(passwordEncoder.encode("Password@123"))
                .phone("0909000000")
                .address("Hanoi")
                .role(role)
                .status("ACTIVE")
                .build();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
