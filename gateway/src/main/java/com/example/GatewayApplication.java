package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.CompositeFilter;

import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
@EnableZuulProxy
@RestController
@EnableOAuth2Client
@EnableRedisRepositories
public class GatewayApplication {

    @Autowired
    private ApplicationUserRepository applicationUserRepository;

    @RequestMapping(value = "/user", method = RequestMethod.GET)
    public Map<String, Object> user(Principal user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", user.getName());
        map.put("roles", AuthorityUtils.authorityListToSet(((Authentication) user)
                .getAuthorities()));
        return map;
    }

    @RequestMapping(value = "/user", method = RequestMethod.POST)
    public void save(ApplicationUser applicationuser, Principal user) {
        applicationuser.setUser(user.getName());
        applicationuser.getGrantedAuthorities().add(new SimpleGrantedAuthority("ROLE_USER"));
        applicationUserRepository.save(applicationuser);
    }

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

	@Configuration
	@Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)
	protected static class SecurityConfiguration extends WebSecurityConfigurerAdapter {

		@Autowired
		OAuth2ClientContext oauth2ClientContext;

        @Autowired
        private ApplicationUserRepository applicationUserRepository;

        @Bean
		@ConfigurationProperties("authserver")
		public ClientResources authserver() {
			return new ClientResources();
		}

        @Bean
        @ConfigurationProperties("github")
        public ClientResources github() {
            return new ClientResources();
        }

		@Bean
		@ConfigurationProperties("facebook")
		public ClientResources facebook() {
			return new ClientResources();
		}

		@Bean
		public FilterRegistrationBean oauth2ClientFilterRegistration(
				OAuth2ClientContextFilter filter) {
			FilterRegistrationBean registration = new FilterRegistrationBean();
			registration.setFilter(filter);
			registration.setOrder(-100);
			return registration;
		}

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http
					.antMatcher("/**")
					.authorizeRequests()
					.antMatchers("/", "/login**", "/webjars/**")
					.permitAll()
					.anyRequest()
					.authenticated()
					.and().exceptionHandling()
					.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/"))
					.and().logout().logoutSuccessUrl("/").permitAll()
					.and().csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
					.and().addFilterBefore(ssoFilter(), BasicAuthenticationFilter.class);
		}

		private Filter ssoFilter() {
			CompositeFilter filter = new CompositeFilter();
			List<Filter> filters = new ArrayList<>();
			filters.add(ssoFilter(facebook(), "/login/facebook"));
			filters.add(ssoFilter(github(), "/login/github"));
            filters.add(ssoFilter(authserver(), "/login/authserver"));
			filter.setFilters(filters);
			return filter;
		}

		private Filter ssoFilter(ClientResources client, String path) {
			OAuth2ClientAuthenticationProcessingFilter filter = new OAuth2ClientAuthenticationProcessingFilter(path);
			OAuth2RestTemplate template = new OAuth2RestTemplate(client.getClient(), oauth2ClientContext);
			filter.setRestTemplate(template);
			UserInfoTokenServices tokenServices = new UserInfoTokenServices(
					client.getResource().getUserInfoUri(), client.getClient().getClientId());
			tokenServices.setRestTemplate(template);
            SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
            tokenServices.setAuthoritiesExtractor(map -> getGrantedAuthorities(successHandler, map));
            filter.setAuthenticationSuccessHandler(successHandler);
			filter.setTokenServices(tokenServices);
			return filter;
		}

        private List<GrantedAuthority> getGrantedAuthorities(SavedRequestAwareAuthenticationSuccessHandler successHandler, Map<String, Object> map) {
            PrincipalExtractor principalExtractor = new FixedPrincipalExtractor();
            Object principal = principalExtractor.extractPrincipal(map);
            if (principal instanceof String) {
                ApplicationUser applicationUser = applicationUserRepository.findOne((String) principal);
                if (applicationUser != null) {
                    return applicationUser.getGrantedAuthorities();
                }
            }
            successHandler.setRedirectStrategy(new DefaultRedirectStrategy() {
                @Override
                public void sendRedirect(HttpServletRequest request, HttpServletResponse response, String url) throws IOException {
                    super.sendRedirect(request, response, "/#/register");
                }
            });
            return new FixedAuthoritiesExtractor().extractAuthorities(map);
        }

        class ClientResources {

			@NestedConfigurationProperty
			private AuthorizationCodeResourceDetails client = new AuthorizationCodeResourceDetails();

			@NestedConfigurationProperty
			private ResourceServerProperties resource = new ResourceServerProperties();

			public AuthorizationCodeResourceDetails getClient() {
				return client;
			}

			public ResourceServerProperties getResource() {
				return resource;
			}
		}
	}

}
