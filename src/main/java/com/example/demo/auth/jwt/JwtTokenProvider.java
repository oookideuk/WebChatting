package com.example.demo.auth.jwt;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import com.example.demo.auth.security.CustomUser;
import com.example.demo.exception.InvalidJwtException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.impl.DefaultClaims;

@Component
public class JwtTokenProvider {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	public static final String AUTHORIZATION_HEADER = "Authorization";
	@Value("${app.jwt.secret}") private String secret;
	@Value("${app.jwt.access.type}") private String accessType;
	@Value("${app.jwt.access.valid-second}") private long accessValidSecond;
	@Value("${app.jwt.access.cookie.name}") private String accessCookieName;
	@Value("${app.jwt.refresh.type}") private String refreshType;
	@Value("${app.jwt.refresh.valid-second}") private long refreshValidSecond;
	@Value("${app.jwt.refresh.cookie.name}") private String refreshCookieName;
	
	private UserDetailsService userDetailsService;
	public JwtTokenProvider(UserDetailsService userDetailsService) {
		this.userDetailsService = userDetailsService;
	}
	
	/**
	 * Token을 생성한다.
	 */
	public String generateToken(Claims claims) {
		return Jwts.builder()
				.setClaims(claims)
				.signWith(SignatureAlgorithm.HS256, secret)
				.compact();
	}
	
	/**
	 * Access Claims를 생성한다.
	 */
	public Claims generateAccessClaims(String jti, CustomUser user) {
		LocalDateTime now = LocalDateTime.now();
		Claims claims = new DefaultClaims();
		claims.setId(jti);
		claims.setIssuedAt(Date.from(
				now.atZone(ZoneId.systemDefault()).toInstant() ));
		claims.setExpiration(Date.from(
				now.plusSeconds(accessValidSecond).atZone(ZoneId.systemDefault()).toInstant() ));
//		claims.setExpiration(Date.from(
//				now.plusYears(365).atZone(ZoneId.systemDefault()).toInstant() ));
		claims.put("memberId", user.getMemberId());
		claims.put("memberName", user.getName());
		claims.put("type", accessType);
		return claims;
	}
	
	/**
	 * Refresh Claims를 생성한다.
	 */
	public Claims generateRefreshClaims(String jti, CustomUser user) {
		LocalDateTime now = LocalDateTime.now();
		Claims claims = new DefaultClaims();
		claims.setId(jti);
		claims.setIssuedAt(Date.from(
				now.atZone(ZoneId.systemDefault()).toInstant() ));
		claims.setExpiration(Date.from(
				now.plusSeconds(refreshValidSecond).atZone(ZoneId.systemDefault()).toInstant() ));
//		claims.setExpiration(Date.from(
//				now.plusYears(365).atZone(ZoneId.systemDefault()).toInstant() ));
		claims.put("memberId", user.getMemberId());
		claims.put("type", refreshType);
		return claims;
	}
	
	/**
	 * Access Token인지 확인한다.
	 */
	public boolean isAccessToken(String token) {
		Claims claims = this.getClaims(token);
		String type = (String) claims.get("type");
		return type.equals(accessType);
	}
	
	/**
	 * Refresh Token인지 확인한다.
	 */
	public boolean isRefreshToken(String token) {
		Claims claims = this.getClaims(token);
		String type = (String) claims.get("type");
		return type.equals(refreshType);
	}
	
	/**
	 * 토큰 유효성을 확인한다.
	 */
	public boolean validateToken(String token) {
		this.getClaims(token);
		return true;
	}
	
	/**
	 * token에서 인증 정보를 가져온다.
	 */
	public Authentication getAuthentication(String token) {
		Claims claims = this.getClaims(token);
		String memberId = (String) claims.get("memberId");
		CustomUser user = (CustomUser) userDetailsService.loadUserByUsername(memberId);
		Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
		
		return new UsernamePasswordAuthenticationToken(user, "", authorities);
	}
	
	/**
	 * 토큰 ID를 가져온다.
	 */
	public String getTokenId(String token) {
		Claims claims = this.getClaims(token);
		return claims.getId();
	}
	
	/**
	 * 토큰에서 회원 아이디를 가져온다.
	 */
	public String getMemberId(String token) {
		Claims claims = this.getClaims(token);
		return (String) claims.get("memberId");
	}
	
	/**
	 * 토큰에서 회원 이름을 가져온다.
	 */
	public String getMemberName(String token) {
		Claims claims = this.getClaims(token);
		return (String) claims.get("memberName");
	}
	
	/**
	 * 토큰 만료일을 가져온다.
	 */
	public LocalDateTime getExpirationDate(String token) {
		Claims claims = this.getClaims(token);
		Date expirationDate = claims.getExpiration();
		//convert Date to LocalDateTime
		return Instant.ofEpochMilli(expirationDate.getTime())
				.atZone(ZoneId.systemDefault())
				.toLocalDateTime();
	}
	
	/**
	 * 토큰에서 Claims를 가져온다.
	 */
	private Claims getClaims(String token) {
		try {
			Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
			return claims;
		}catch(ExpiredJwtException e) {
			logger.debug("JWT token is expired[{}] TokenId[{}]", e.getMessage(), e.getClaims().getId());
			throw new InvalidJwtException(e);
		}catch(SignatureException e) {
			logger.error("Invalid JWT signature[{}]", e.getMessage());
			throw new InvalidJwtException(e);
		}catch(IllegalArgumentException e) {
			logger.error("JWT claims string is null or empty[{}]", e.getMessage());
			throw new InvalidJwtException(e);
		}catch(MalformedJwtException e) {
			logger.error("Invalid JWT token[{}]", e.getMessage());
			throw new InvalidJwtException(e);
		}catch(UnsupportedJwtException e) {
			logger.error("JWT token is unsupported[{}]", e.getMessage());
			throw new InvalidJwtException(e);
		}
	}
	
	/**
	 * <p>Authorization Header에서 JWT를 가져온다.</p>
	 * <p>Authorization: type credentials</p>
	 */
	public String getTokenFromHeader(HttpServletRequest request) {
		String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
		return this.getTokenFromHeader(authorizationHeader);
	}
	
	public String getTokenFromHeader(String authorizationHeader) {
		if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7, authorizationHeader.length());
        }else {
        	logger.debug("Authorization[{}]", authorizationHeader);
        	logger.debug("Authorization does not begin with Bearer String");
        }
        return null;
	}
	
	/**
	 * <p>Cookie에서 Access Token을 가져온다.</p>
	 * <p>JWT가 없으면 null을 반환한다.</p>
	 */
	public String getAccessTokenFromCookie(HttpServletRequest request) {
		Cookie[] cookies =  request.getCookies();
		if(cookies == null) {
			return null;
		}
		
		for(Cookie cookie : cookies) {
			if(accessCookieName.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}
	
	/**
	 * <p>Cookie에서 Refresh Token을 가져온다.</p>
	 * <p>JWT가 없으면 null을 반환한다.</p>
	 */
	public String getRefreshTokenFromCookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if(cookies == null) {
			return null;
		}
		
		for(Cookie cookie : cookies) {
			if(refreshCookieName.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}
	
	/**
	 * access token을 저장한 cookie를 생성한다.
	 */
	public HttpCookie generateAccessCookie(String token) {
		return ResponseCookie.from(accessCookieName, token)
                .maxAge(60*60*24*365)
                .path("/")
                .build();
	}
	
	/**
	 * refresh token을 저장한 cookie를 생성한다.
	 */
	public HttpCookie generateRefreshCookie(String token) {
		return ResponseCookie.from(refreshCookieName, token)
                .maxAge(60*60*24*365)
                .httpOnly(true)
                .path("/")
                .build();
	}
}
