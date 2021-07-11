package com.example.demo.domain.entitiy;

import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.demo.domain.Role;
import com.example.demo.dto.MemberDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter @ToString(exclude = {"profilePictures", "jwtTokens","chatParticipants"})
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "member")
public class MemberEntity {
	@Transient
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	@Id
	@Column(name="member_id")
    private String memberId;
    private String password;
    private String name;
    @Enumerated(EnumType.STRING)
    private Role role;
    private String email;
    private String emailAuthKey;
    private int emailAuthFlag;
    private LocalDateTime registerDate;
    
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<ProfilePictureEntity> profilePictures;
    @OneToMany(mappedBy = "member", cascade = CascadeType.REMOVE)
    private List<JwtTokenEntity> jwtTokens;
    @OneToMany(mappedBy = "pk.participantId", cascade = CascadeType.REMOVE)
    private List<ChatParticipantEntity> chatParticipants;
    
    
    public MemberEntity updatePersonalInformation(MemberDTO.PersonalInfomationModificationReq req) {
    	this.name = req.getName();
    	return this;
    }
    
    public MemberEntity updatePassword(String password) {
    	this.password = password;
    	return this;
    }
    
    public MemberEntity enableEmailAuthFlag() {
    	this.emailAuthFlag = 1;
    	return this;
    }
}
