package com.example.demo.service.impl;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.LoggerFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.auth.jwt.JwtTokenProvider;
import com.example.demo.auth.security.CustomUser;
import com.example.demo.domain.entitiy.JwtTokenEntity;
import com.example.demo.domain.entitiy.MemberEntity;
import com.example.demo.domain.repository.JwtTokenRepository;
import com.example.demo.dto.AuthDTO;
import com.example.demo.exception.RefreshFailureException;
import com.example.demo.exception.TokenNotFoundException;
import com.example.demo.service.AuthService;

import ch.qos.logback.classic.Logger;
import io.jsonwebtoken.Claims;

@Service
public class AuthServiceImpl implements AuthService {
	private final Logger logger = (Logger) LoggerFactory.getLogger(this.getClass());
	private JwtTokenRepository jwtTokenRepo;
	private UserDetailsService userDetailService;
	private PasswordEncoder passwordEncoder;
	private JwtTokenProvider jwtTokenProvider;

	public AuthServiceImpl(JwtTokenRepository jwtTokenRepo,
			UserDetailsService userDetailService, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
		this.jwtTokenRepo = jwtTokenRepo;
		this.userDetailService = userDetailService;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenProvider = jwtTokenProvider;
	}
	
	@Override
	@Transactional(rollbackFor = Exception.class)
	public ResponseEntity<AuthDTO.JwtAuthRes> login(AuthDTO.LoginReq loginReq){
		//회원 검증을 진행한다.
		CustomUser customUser = (CustomUser) userDetailService.loadUserByUsername(loginReq.getMemberId());
		if(!passwordEncoder.matches(loginReq.getPassword(), customUser.getPassword())) {
			throw new BadCredentialsException(customUser.getMemberId() + " 패스워드 불일치");
		}else if(customUser.getEmailAuthFlag() == 0) {
			throw new DisabledException(customUser.getMemberId() + " 이메일 미인증");
		}
		
		//토큰 아이디를 생성한다.
		String tokenId = this.generateTokenId();
		//토큰을 생성한다.
		String accessToken = this.generateAccessToken(tokenId, customUser);
		String refreshToken = this.generateRefreshToken(tokenId, customUser);
		//token 정보를 DB에 저장한다.
		this.saveJwtToken(customUser.getMemberId(), refreshToken);
		//token을 cookie에 저장한다.
		HttpHeaders httpHeaders = new HttpHeaders();
		this.addAccessTokenCookie(httpHeaders, accessToken);
		this.addRefreshTokenCookie(httpHeaders, refreshToken);
		//응답을 생성한다.
		AuthDTO.JwtAuthRes jwtAuthRes =	AuthDTO.JwtAuthRes.builder()
				.accessToken(accessToken)
				.refreshToken(refreshToken).build();
		
		
		logger.debug("------------------------------------------------------------");
		logger.debug("responseEntity [{}]", new ResponseEntity<AuthDTO.JwtAuthRes>(jwtAuthRes, httpHeaders, HttpStatus.OK));
		return new ResponseEntity<AuthDTO.JwtAuthRes>(jwtAuthRes, httpHeaders, HttpStatus.OK);
	}
	
	/**
	 * Token ID를 생성한다.
	 */
	private String generateTokenId() {
		return UUID.randomUUID().toString();
	}
	
	/**
	 * 액세스 토큰을 생성한다.
	 */
	private String generateAccessToken(String jti, CustomUser customUser) {
		Claims accessClaims = jwtTokenProvider.generateAccessClaims(jti, customUser);
		return jwtTokenProvider.generateToken(accessClaims);
	}
	
	/**
	 * 리프레시 토큰을 생성한다.
	 */
	private String generateRefreshToken(String jti, CustomUser customUser) {
		Claims refreshClaims = jwtTokenProvider.generateRefreshClaims(jti, customUser);
		return jwtTokenProvider.generateToken(refreshClaims);
	}
	
	/**
	 * 액세스 토큰을 쿠키에 저장한다.
	 */
	private void addAccessTokenCookie(HttpHeaders httpHeaders, String accessToken) {
		HttpCookie accessCookie = jwtTokenProvider.generateAccessCookie(accessToken);
		httpHeaders.add(HttpHeaders.SET_COOKIE, accessCookie.toString());
	}
	
	/**
	 * 리프레시 토큰을 쿠키에 저장한다.
	 */
	private void addRefreshTokenCookie(HttpHeaders httpHeaders, String refreshToken) {
		HttpCookie refreshCookie = jwtTokenProvider.generateRefreshCookie(refreshToken);
		httpHeaders.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());
	}
	
	/**
	 * 토큰 정보를 DB에 저장한다.
	 */
	@Transactional
	private JwtTokenEntity saveJwtToken(String memberId, String refreshToken) {
		String jti = jwtTokenProvider.getTokenId(refreshToken);
		LocalDateTime expirationDate = jwtTokenProvider.getExpirationDate(refreshToken);
		
		JwtTokenEntity jwtTokenEntity = JwtTokenEntity.builder()
				.member(MemberEntity.builder().memberId(memberId).build())
				.tokenId(jti)
				.expirationDate(expirationDate)
				.build();
		return jwtTokenRepo.save(jwtTokenEntity);
	}
	
	@Transactional(rollbackFor = Exception.class)
	@Override
	public String logout(HttpServletRequest request, HttpServletResponse response) {
		//DB에서 토큰 정보 삭제
		String accessToken = jwtTokenProvider.getTokenFromHeader(request);
		String tokenId = jwtTokenProvider.getTokenId(accessToken);
		JwtTokenEntity jwtToken = this.findJwtTokenById(tokenId);	//DB에 토큰 정보 없으면 TokenNotFoundException 발생한다.
		String memberId = jwtToken.getMember().getMemberId();
		this.deleteJwtTokenById(jwtToken);
		//세션 및 쿠키 삭제		
		this.deleteSession(request);
		this.deleteCookies(request, response);

		return memberId;
	}
	
	/**
	 * DB에서 토큰 정보를 가져온다.
	 */
	private JwtTokenEntity findJwtTokenById(String tokenId) {
		return jwtTokenRepo.findById(tokenId).orElseThrow( () -> new TokenNotFoundException("tokenId : " + tokenId));
	}
	
	/**
	 * DB에서 토큰 정보를 삭제한다.
	 */
	private void deleteJwtTokenById(JwtTokenEntity jwtToken) {
		jwtTokenRepo.delete(jwtToken);
	}
	
	/**
	 * 세션 정보를 삭제한다.
	 */
	private void deleteSession(HttpServletRequest request) {
		SecurityContextHolder.clearContext();
		HttpSession session = request.getSession(false);
		logger.debug("session[{}]", session);
		if(session != null) {
			session.invalidate();
		}
	}
	
	/**
	 * 쿠키를 삭제한다.
	 */
	private void deleteCookies(HttpServletRequest request, HttpServletResponse response) {
		if(request.getCookies() != null) {
			for(Cookie cookie : request.getCookies()) {
				logger.debug("deleted cookieName[{}]", cookie.getName());
				cookie.setMaxAge(0);
				cookie.setValue("");
				cookie.setPath("/");
				response.addCookie(cookie);
			}
		}
	}
	
	@Transactional(rollbackFor = Exception.class)
	@Override
	public ResponseEntity<AuthDTO.JwtAuthRes> refresh(HttpServletRequest request) {
		HttpHeaders httpHeaders = new HttpHeaders();
		try {
			String refreshToken = jwtTokenProvider.getRefreshTokenFromCookie(request);
			
			//리프레시 토큰 검증한다.
			if(jwtTokenProvider.validateToken(refreshToken) && jwtTokenProvider.isRefreshToken(refreshToken)) {
				String tokenId = jwtTokenProvider.getTokenId(refreshToken);
				JwtTokenEntity jwtToken = this.findJwtTokenByTokenIdForUpdate(tokenId);
				CustomUser user = (CustomUser) userDetailService.loadUserByUsername(jwtToken.getMember().getMemberId());

				//refresh count+1한다.
				jwtToken.increaseRefreshCount();
				//리프레시 토큰 만료일이 인접해있다면 새로운 리프레시 토큰을 발급한다.
				if(this.isExpirationDateAdjacent(refreshToken)) {
					logger.debug("리프레시 토큰 갱신");
					//새로운 리프레시 토큰을 생성한다.
					refreshToken = this.generateRefreshToken(tokenId, user);
					//만료일을 변경한다.
					LocalDateTime newExpirationDate = jwtTokenProvider.getExpirationDate(refreshToken);
					jwtToken.changeExpirationDate(newExpirationDate);
					//새로운 리프레시 토큰을 쿠키에 저장한다.
					this.addRefreshTokenCookie(httpHeaders, refreshToken);
				}
				
				//액세스 토큰을 생성한다.
				String accessToken = this.generateAccessToken(tokenId, user);
				//액세스 토큰을 쿠키에 저장한다.
				this.addAccessTokenCookie(httpHeaders, accessToken);
				
				AuthDTO.JwtAuthRes jwtAuthRes =	AuthDTO.JwtAuthRes.builder()
						.accessToken(accessToken)
						.refreshToken(refreshToken).build();
				logger.debug("access token 만료일[{}]", jwtTokenProvider.getExpirationDate(accessToken));
				logger.debug("refresh token 만료일[{}]", jwtTokenProvider.getExpirationDate(refreshToken));
				logger.debug("------------------------------------- 리프레시 성공");
				return new ResponseEntity<AuthDTO.JwtAuthRes>(jwtAuthRes, httpHeaders, HttpStatus.OK);
			}
		}catch(Exception e) {
			logger.debug("리프레시 실패[{}]", e.getMessage());
			throw new RefreshFailureException(e);
		}
		
		throw new RefreshFailureException();
	}
	
	/**
	 * 토큰 정보 수정을 위해 DB에서 토큰 정보를 가져온다.
	 */
	private JwtTokenEntity findJwtTokenByTokenIdForUpdate(String tokenId) {
		return jwtTokenRepo.findByTokenIdForUpdate(tokenId).orElseThrow(() -> new TokenNotFoundException("tokenId : " + tokenId));
	}
	
	/**
	 * 만료일이 인접해있는지 확인한다.
	 * 만료일이 기준일 이전이면 true를 반환한다.
	 */
	private boolean isExpirationDateAdjacent(String token) {
		LocalDateTime baseDate = LocalDateTime.now().plusDays(14);
		LocalDateTime expirationDate = jwtTokenProvider.getExpirationDate(token);
		logger.debug("now[{}]", LocalDateTime.now());
		logger.debug("baseDate[{}]", baseDate);
		logger.debug("expirationDate[{}]", expirationDate);
		return expirationDate.isBefore(baseDate);
		
	}
	
}
