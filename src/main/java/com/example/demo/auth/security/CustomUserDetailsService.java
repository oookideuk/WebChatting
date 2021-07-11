package com.example.demo.auth.security;

import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.demo.domain.entitiy.MemberEntity;
import com.example.demo.domain.repository.MemberRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService{
	MemberRepository memberRepository;
	
	public CustomUserDetailsService(MemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
		Optional<MemberEntity> memberOpt = memberRepository.findById(userId);
		MemberEntity member = memberOpt.orElseThrow(() -> new UsernameNotFoundException(userId + " 계정 없음"));
		
		return new CustomUser(member);	
	}
}
