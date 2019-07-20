/*
 * Created on 20 Jul 2018
 */
package ch.want.devtaskhub;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import ch.want.devtaskhub.state.UserProperties;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserProperties userProperties;

    @Override
    protected void configure(final HttpSecurity http) throws Exception {
        http.csrf().disable()
                .authorizeRequests().antMatchers("/assets/**", "/webhooks/**")
                .permitAll();
        if (StringUtils.isNoneBlank(userProperties.getUsername(), userProperties.getPassword())) {
            http
                    .antMatcher("/*")
                    .authorizeRequests()
                    .anyRequest().authenticated()
                    .and()
                    .httpBasic();
        }
    }

    @Autowired
    public void configureGlobal(final AuthenticationManagerBuilder auth) throws Exception {
        if (StringUtils.isNoneBlank(userProperties.getUsername(), userProperties.getPassword())) {
            // Note that Spring Security 5 required a password encoder. Prefixing the password
            // with {noop} triggers the NoOpPasswordEncoder
            auth.inMemoryAuthentication()
                    .withUser(userProperties.getUsername()).password("{noop}" + userProperties.getPassword())
                    .authorities("ROLE_USER");
        }
    }
}
