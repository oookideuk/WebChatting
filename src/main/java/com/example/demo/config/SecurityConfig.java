package com.example.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.demo.auth.jwt.JwtAuthenticationFilter;
import com.example.demo.auth.jwt.JwtTokenProvider;
import com.example.demo.auth.security.CustomAccessDeniedHandler;
import com.example.demo.auth.security.CustomAuthenticationEntryPoint;
import com.example.demo.auth.security.CustomAuthenticationProvider;
import com.example.demo.auth.security.CustomUserDetailsService;
import com.example.demo.domain.Role;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
	@Autowired
	CustomUserDetailsService customUserDetailsService;
	
	@Autowired
	CustomAuthenticationProvider customAuthenticationProvider;
	
	@Autowired
	CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
	
	@Autowired
	CustomAccessDeniedHandler customAccessDeniedHandler;
	
	@Autowired
	JwtTokenProvider jwtTokenProvider;
	
	@Autowired
	CorsConfigurationSource corsConfigurationSource;
	
	@Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
	
	@Override
    public void configure(WebSecurity web) throws Exception
    {
        web.ignoring().antMatchers("/css/**", "/js/**", "/img/**", "/lib/**");
        web.ignoring().antMatchers(HttpMethod.POST, "/v1/members"); //회원가입
        web.ignoring().antMatchers(HttpMethod.GET, "/v1/chat/download/**");	//채팅 파일 다운로드
        web.ignoring().antMatchers(HttpMethod.GET, "/v1/members/me/email/auth"); //이메일 인증
        web.ignoring().antMatchers(HttpMethod.GET, "/v1/file/profilePicture/**");//프로필 사진 보기
        //회원가입
        //로그인
        //아이디
        //비밀번호
    }
	
	@Override
    protected void configure(HttpSecurity http) throws Exception {
		http
		.csrf().disable()
		/*
		 * - SecurityContextPersistenceFilter 인증 후 : 
		 * 		- form login : UsernamePasswordAuthenticationFilter에서 SecurityContextHolder에 등록한 Authentication을 임시변수(contextAfterChainExecution)에 저장한 후 SecurityContextHolder를 초기화한다.
		 *  				   Security Session을 사용하면 임시 저장한 인증객체를 SecurityContextHolder에 저장한다.
		 *  	- jwt : jwt filter에서 SecurityContextHolder에 등록한 Authentication을 임시변수(contextAfterChainExecution)에 저장한 후 SecurityContextHolder를 초기화한다.
		 * 				SessionCreationPolicy.STATELESS를 사용하면 임시 저장한 Authentication을 Session에 저장하지 않는다.
		 * - SessionCreationPolicy.STATELESS를 사용하면 NullSecurityContextRepository를 사용한다.
		 */
		.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
		.and()
		.cors().configurationSource(corsConfigurationSource)
		.and()
		.headers()
        	.frameOptions().sameOrigin() // SockJS는 기본적으로 HTML iframe 요소를 통한 전송을 허용하지 않도록 설정되는데 해당 내용을 해제한다.
//        .and()
//		.formLogin()
//			.loginPage("/login")
//			.loginProcessingUrl("/v1/login")
//			.usernameParameter("memberId")
//			.passwordParameter("password")
//			.defaultSuccessUrl("/v1/login/success")
//			//.failureHandler(customAuthenticationFailureHandler)	//로그인 실패 예외처리 등록
//		.and()
//		.logout()
//			.logoutUrl("/v1/logout")
//			.invalidateHttpSession(true)
//			.logoutSuccessUrl("/v1/logout/success")
		.and()
		.authorizeRequests()
			//.antMatchers("/v1/**").hasAnyAuthority(Role.ROLE_MEMBER.getValue(),Role.ROLE_ADMIN.getValue())
			//.antMatchers("/chat/**").hasAnyAuthority(Role.ROLE_MEMBER.getValue(),Role.ROLE_ADMIN.getValue())
			.antMatchers("/v1/members/**").hasAnyAuthority(Role.ROLE_MEMBER.getValue(),Role.ROLE_ADMIN.getValue())
			.antMatchers("/v1/profilePicture/**").hasAnyAuthority(Role.ROLE_MEMBER.getValue(),Role.ROLE_ADMIN.getValue())
	//		.antMatchers("/v1/image/**").hasAnyAuthority(Role.ROLE_MEMBER.getValue(),Role.ROLE_ADMIN.getValue())
	//		.antMatchers("/v1/file/**").hasAnyAuthority(Role.ROLE_MEMBER.getValue(),Role.ROLE_ADMIN.getValue())
			.antMatchers("/admins/**").hasAuthority(Role.ROLE_ADMIN.getValue())
	//		.antMatchers("/members/**").hasAnyAuthority(Role.ROLE_MEMBER.getValue(),Role.ROLE_ADMIN.getValue())
			.antMatchers("/v1/chat/**").hasAnyAuthority(Role.ROLE_MEMBER.getValue(),Role.ROLE_ADMIN.getValue())
	//		.antMatchers("/v1/auth/logout", "/v1/auth/me").hasAnyAuthority(Role.ROLE_MEMBER.getValue(),Role.ROLE_ADMIN.getValue())
			.antMatchers("/**").permitAll()
			
		/*
		 * UsernamePasswordAuthenticationFilter :
		 * 	formLogin()을 설정하면 기본적으로 UsernamePasswordAuthenticationFilter를 이용한다. (Request is to process authentication 로그 찍힘)
		 * 	로그인 URL인지 확인하고 로그인 요청이라면 AuthenticationProvider를 통해 Authentication을 생성한 후 SecurityContextHolder에 등록한다.
		 */
		.and()
		.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);		

		//미인증 예외처리 등록
		http.exceptionHandling().authenticationEntryPoint(customAuthenticationEntryPoint);
		//권한없음 예외처리 등록
		http.exceptionHandling().accessDeniedHandler(customAccessDeniedHandler);
    }
	
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.authenticationProvider(customAuthenticationProvider);
	}
	
	// CORS 허용 적용
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.addAllowedOrigin("*");
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
