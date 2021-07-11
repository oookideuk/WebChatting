package com.example.demo.domain.entitiy;

import java.io.Serializable;

import javax.persistence.Embeddable;

import lombok.Builder;
import lombok.Data;

@Data
@Embeddable
public class ChatParticipantPK implements Serializable{
	private static final long serialVersionUID = 4326737045107525090L;
	private String roomId;
	private String participantId;
	
	public ChatParticipantPK() {}
	
	@Builder
	public ChatParticipantPK(String roomId, String participantId) {
		this.roomId = roomId;
		this.participantId = participantId;
	}
	
	
}
