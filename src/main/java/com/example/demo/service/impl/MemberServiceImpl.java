package com.example.demo.service.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.mail.MessagingException;
import javax.persistence.EntityNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.common.CustomMailSender;
import com.example.demo.common.TempKey;
import com.example.demo.domain.Role;
import com.example.demo.domain.entitiy.MemberEntity;
import com.example.demo.domain.repository.MemberRepository;
import com.example.demo.dto.MemberDTO;
import com.example.demo.exception.AccountDuplicationException;
import com.example.demo.exception.AccountNotFoundException;
import com.example.demo.exception.PasswordMismathException;
import com.example.demo.service.MemberService;
import com.example.demo.service.ProfilePictureService;

@Service
public class MemberServiceImpl implements MemberService {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private MemberRepository memberRepository;
	private PasswordEncoder passwordEncoder;
	private ProfilePictureService profilePictureService;
	private CustomMailSender mailSender;
	
	public MemberServiceImpl(MemberRepository memberRepository, PasswordEncoder passwordEncoder, ProfilePictureService profilePictureService
								,CustomMailSender mailSender) {
		this.passwordEncoder = passwordEncoder;
		this.memberRepository = memberRepository;
		this.profilePictureService = profilePictureService;
		this.mailSender = mailSender;
	}
	
	@Override
	@Transactional(rollbackFor = Exception.class)
	public MemberDTO.Response signup(MemberDTO.SignupReq signupReq) throws MessagingException, IOException {
		MemberDTO.Response memberRes = null;
		
		//회원정보를 DB에 저장한다.
		memberRes = MemberDTO.Response.of(this.saveMember(signupReq));
		// 회원가입 인증 메일을 전송한다.
		// saveMembe 함수에서 이메일 인증키를 설정하기 때문에 회원정보를 DB에 저장한 후 처리한다.
		this.sendAuthMail(signupReq);
		
		return memberRes;
	}
	
	/**
	 * 회원정보를 DB에 저장한다.
	 */
	private MemberEntity saveMember(MemberDTO.SignupReq signupReq) {
		String encodedPassword = passwordEncoder.encode(signupReq.getPassword()); //패스워드를 암호화한다.
		signupReq.setEncodedPassword(encodedPassword);
		signupReq.setRole(Role.ROLE_MEMBER); //권한을 설정한다.
		String authEmailKey = new TempKey().getKey(8, true); //랜덤한 이메일 인증키를 생성한다.
		signupReq.setEmailAuthKey(authEmailKey);
		
		MemberEntity memberEntity = signupReq.toEntity();
		return memberRepository.save(memberEntity);
	}
	
	/**
	 * 회원가입 인증 메일을 전송한다.
	 */
	private void sendAuthMail(MemberDTO.SignupReq signupReq) throws MessagingException {
		logger.debug("------------------------------이메일 전송 시작");
		String subject = "[chatting] 이메일 인증을 완료해주세요.";
		String path = "/mail/signupAuth";
		Map<String, Object> contextMap = new HashMap<String, Object>();
		contextMap.put("memberId", signupReq.getMemberId());
		contextMap.put("emailAuthKey", signupReq.getEmailAuthKey());
		
		mailSender.sendMail(subject, signupReq.getEmail(), contextMap, path);
		logger.debug("------------------------------이메일 전송 끝");	
	}
	
	@Override
	public MemberDTO.Response findMember(String memberId) {
		Optional<MemberEntity> entityOpt = memberRepository.findById(memberId);
		MemberEntity entity = entityOpt.orElseThrow(() -> new EntityNotFoundException("memberEntity with memberId : " + memberId));
		
		return MemberDTO.Response.of(entity);
	}
	
	@Override
	@Transactional
	public void deleteMember(String memberId) throws IOException {
		Optional<MemberEntity> entityOpt = memberRepository.findById(memberId);
		MemberEntity entity = entityOpt.orElseThrow(() -> new EntityNotFoundException("memberEntity with memberId : " + memberId));
		
		//등록된 프로필 사진이 있으면 삭제한다.
		if(profilePictureService.existsByMemberId(memberId)){
			profilePictureService.deleteProfilePictureByMemberId(memberId);
		}
		/*TODO 회원탈퇴시 채팅 관련 로직 처러하기
		 * chatParticipant 테이블에서 참가자 지우기
		 * 메시지와 파일은 어떻게 처리할 것인가?
		 * 		지우지 말 것.
		 * 		null로 처리.
		 */
		
		memberRepository.delete(entity); //회원을 삭제한다.
	}
	
	@Override
	@Transactional
	public MemberDTO.Response updatePersonalInformation(MemberDTO.PersonalInfomationModificationReq req) {
		Optional<MemberEntity> entityOpt = memberRepository.findById(req.getMemberId());
		MemberEntity entity = entityOpt.map(m -> m.updatePersonalInformation(req))
									.orElseThrow(() -> new EntityNotFoundException("memberEntity with memberId : " + req.getMemberId()));
		return MemberDTO.Response.of(entity);
	}
	
	@Override
	@Transactional
	public void updatePassword(MemberDTO.passwordModificationReq req) {
		Optional<MemberEntity> entityOpt = memberRepository.findById(req.getMemberId());
		MemberEntity entity = entityOpt.orElseThrow(() -> new EntityNotFoundException("memberEntity with memberId : " + req.getMemberId()));
		//패스워드 일치여부 확인
		if(!passwordEncoder.matches(req.getPassword(), entity.getPassword())) {
			throw new PasswordMismathException("input password : " + req.getPassword());
		}
		entity.updatePassword(passwordEncoder.encode(req.getPasswordChange()));
	}
	
	@Override
	@Transactional
	public void enableEmailAuthFlag(String memberId, String emailAuthKey) {
		Optional<MemberEntity> memberOpt = memberRepository.findById(memberId);
		if(!memberOpt.isPresent()) {
			throw new EntityNotFoundException("memberEntity with memberId : " + memberId);
		}
		
		MemberEntity member = memberOpt.get();
		if(!emailAuthKey.equals(member.getEmailAuthKey())) {
			throw new EntityNotFoundException("인증키 다름 requestKey : " +emailAuthKey + " storedKey : " + member.getEmailAuthKey())  ;
		}
		member.enableEmailAuthFlag();
	}
	
	@Override
	public void checkRegisteredMember(String memberId) {
		if(!this.existsByMemberId(memberId)) {
			throw new AccountNotFoundException(memberId);
		}
	}
	
	@Override
	public void checkDuplicatedMember(String memberId) {
		if(this.existsByMemberId(memberId)) {
			throw new AccountDuplicationException(memberId);
		}
	}
	
	@Override
	public boolean existsByMemberId(String memberId) {
		return memberRepository.existsById(memberId) ? true : false; 
	}
}
