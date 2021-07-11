package com.example.demo.domain.entitiy;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.FetchType;
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
@Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "jwt_token")
public class JwtTokenEntity {
	@Id
	private String tokenId;
	private LocalDateTime expirationDate;
	private int refreshCount;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id")
	private MemberEntity member;
	
	/**
	 * refresh count +1 한다.
	 */
	public void increaseRefreshCount() {
		this.refreshCount += 1;
	}
	
	/**
	 * 만료일을 변경한다.
	 */
	public void changeExpirationDate(LocalDateTime newExpirationDate) {
		this.expirationDate = newExpirationDate;
	}
}
