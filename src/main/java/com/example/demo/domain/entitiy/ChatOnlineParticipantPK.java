package com.example.demo.domain.entitiy;

import java.io.Serializable;

import javax.persistence.Embeddable;

import lombok.Builder;
import lombok.Data;

@Data
@Embeddable
public class ChatOnlineParticipantPK implements Serializable{
	private static final long serialVersionUID = 7352419115057448116L;
	private String sessionId;
	private String hostAddress;
	private int port;
	
	public ChatOnlineParticipantPK() {}

	@Builder
	public ChatOnlineParticipantPK(String sessionId, String hostAddress, int port) {
		this.sessionId = sessionId;
		this.hostAddress = hostAddress;
		this.port = port;
	}
}
