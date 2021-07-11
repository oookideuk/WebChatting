package com.example.demo.domain.repository;

import java.util.Optional;

import javax.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.entitiy.JwtTokenEntity;

@Repository
public interface JwtTokenRepository extends JpaRepository<JwtTokenEntity, String>{
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from JwtTokenEntity t where t.tokenId = :tokenId")
	Optional<JwtTokenEntity> findByTokenIdForUpdate(@Param("tokenId") String tokenId);
}
