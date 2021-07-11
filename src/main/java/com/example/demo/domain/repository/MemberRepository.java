package com.example.demo.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.entitiy.MemberEntity;

@Repository
public interface MemberRepository extends JpaRepository<MemberEntity, String> {

	List<MemberEntity> findByName(String string);
	
}
