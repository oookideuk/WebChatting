package com.example.demo.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.entitiy.MemberEntity;
import com.example.demo.domain.entitiy.ProfilePictureEntity;

@Repository
public interface ProfilePictureRepository extends JpaRepository<ProfilePictureEntity, Long> {

	/**
	 * 회원의 프로필 사진 목록을 가져온다.
	 */
	List<ProfilePictureEntity> findByMember(MemberEntity member);
	
	/**
	 * 회원의 프로필 사진 목록을 삭제한다.
	 */
	void deleteByMember(MemberEntity member);
	
	/**
	 * 회원의 등록된 프로필 사진이 있는지 확인한다.
	 */
	boolean existsByMember(MemberEntity member);
}
