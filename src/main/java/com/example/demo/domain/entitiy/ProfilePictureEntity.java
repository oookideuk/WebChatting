package com.example.demo.domain.entitiy;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter @ToString(exclude = {"member"})
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "profile_picture")
public class ProfilePictureEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long fileId;	// DB에서 자동증가로 처리한다.
	private String storedFileName;	// 서버에 저장된 파일 이름.
	private String originalFileName;	// 원본 파일 이름.
	private long size;
	private String uploadPath;
	private LocalDateTime registerDate;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id")
	private MemberEntity member;
}
