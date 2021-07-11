<template>
	<div id="chatRoom">
	<b-container>
		<b-row>
			<b-col cols="6">
			</b-col>
			
			<b-col cols="6" style="text-align: right; padding: 0;">
				<a href="#" v-on:click="leaveRoom">
					<span style="padding-right: 5px;">나가기</span>
				</a> 
			</b-col>
		</b-row>
		<b-row>
			<!-- 채팅 방 참가자 목록 -->
			<b-col cols="4" style="padding: 0; border: 1px solid; border-right-style: none;">
				<div class="overflow-auto"  style="height: 60vh;">
					<div v-for="participant in chatParticipants" v-bind:key="participant.participantId">
						<b-list-group-item class="d-flex align-items-center">
							<b-avatar :src="participant.profilePictureUrl" class="mr-3"/>
							<span style="font-size: 16px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">{{ participant.participantName }}</span>
						</b-list-group-item>
					</div>
					
				</div>
			</b-col>

			<b-col cols="8" style="border: 1px solid;">
				<b-row>
					<b-col style="padding: 0;">
						<div ref="messageWindow" id="messageWindow" style="height: 60vh; overflow-x: hidden; word-wrap:break-word; word-break:break-all;">
							<!-- 메시지 리스트 스크롤 페이징 적용 -->
							<infinite-loading direction="top" @infinite="messageInfiniteHandler">
								<div slot="no-more" style="color: rgb(102, 102, 102); font-size: 14px; padding: 10px 0px;"></div>
							</infinite-loading>
							
							<!-- 메시지 목록 -->
							<div v-for="message in messages" v-bind:key="message.messageId">
								<!-- 알림 메시지 -->
								<div v-if="message.type === 'ENTER' || message.type === 'LEAVE'" style="text-align: center; width:100%; margin-bottom: 10px;">
									<div class="myMessage" style="max-width:70%; display: inline-block; background-color: #F0F8FF; border-radius: 25px; padding: 10px;">
										<span>{{ message.message }}</span>
									</div>
								</div>
								<!-- 내가 보낸 메시지 -->
								<div v-else-if="message.sender === $store.getters.getMemberId" class="right" style="text-align:right; margin-bottom:5px;">
									<div style="display: inline-block; width:100%;">
										<b-list-group-item class="d-flex align-items-top" style="border:0; padding:10px 20px 0 0;">
											<div v-if="message.type === 'TEXT'" style="width:100%;">
												<div class="messageDate" style="vertical-align: bottom; display: inline-block">
													<span><small>{{ message.createdDate }}</small></span>
												</div>
												<div class="myMessage" style="max-width:70%; display: inline-block; text-align: left; background-color: #00BFFF; border-radius: 15px; padding: 10px;">
													<span>{{ message.message }}</span>
												</div>
											</div>
											<div v-else-if="message.type === 'IMAGE'" style="width:100%;">
												<div class="messageDate" style="vertical-align: bottom; display: inline-block">
													<span><small>{{ message.createdDate }}</small></span>
												</div>
												<div class="myMessage" style="max-width:70%; display: inline-block;">
													<a :href="message.attachUrl"><img :src="message.attachUrl" style="height:150px; border-radius: 10px;"></a>
												</div>
											</div>
											<div v-else-if="message.type === 'FILE'" style="width: 100%">
												<div class="messageDate" style="vertical-align: bottom; display: inline-block">
													<span><small>{{ message.createdDate }}</small></span>
												</div>
												<a :href="message.attachUrl" download style="color:black;">
													<div style="width:200px; height:50px; border-radius: 10px; background-color: #DCDCDC; padding:12px; display:inline-block; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; text-align:left;">
														<b-icon icon="file-text-fill" scale="1.5" style="margin-right:10px;"></b-icon>
														{{ getFileName(message.attachUrl)}}
													</div>
												</a>
											</div>	
										</b-list-group-item>
									</div>	
								</div>
								
								<!-- 다른 사람이 보낸 메시지 -->
								<div v-else class="left">
									<b-list-group-item class="d-flex align-items-top" style="border: 0; padding: 10px 0 0 20px; margin-bottom:5px;">
										<!-- 채팅방 참가자 목록에서 상대방 프로필 사진 Url을 가져온다. -->
										<b-avatar v-if="chatParticipants.find(p => p.participantId === message.sender)" :src="chatParticipants.find(p => p.participantId === message.sender).profilePictureUrl" class="mr-2"/>
										<b-avatar v-else class="mr-2"/>
										<div v-if="message.type === 'TEXT'" style="text-align: left; width: 100%;">
											<div class="participantName">
												<span>{{ message.name }}</span>
											</div>
											<div class="otherMessage" style="display: inline-block; background-color: #DCDCDC; border-radius: 15px; padding: 10px; max-width:70%;">
												<span>{{ message.message }}</span>
											</div>
											<div class="messageDate" style="vertical-align: bottom; display: inline-block">
												<span><small>{{ message.createdDate }}</small></span>
											</div>
										</div>
										<div v-else-if="message.type === 'IMAGE'">
											<div class="otherMessage" style="max-width:70%; display: inline-block;">
												<a :href="message.attachUrl"><img :src="message.attachUrl" style="height:150px; border-radius: 10px;"></a>
											</div>
											<div class="messageDate" style="vertical-align: bottom; display: inline-block">
													<span><small>{{ message.createdDate }}</small></span>
											</div>
										
										</div>
										<div v-else-if="message.type === 'FILE'">
											<a :href="message.attachUrl" download style="color:black;">
												<div style="width:200px; height:50px; border-radius: 10px; background-color: #DCDCDC; padding:12px; display:inline-block; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; text-align:left;">
													<b-icon icon="file-text-fill" scale="1.5" style="margin-right:10px;"></b-icon>
													{{ getFileName(message.attachUrl)}}
												</div>
											</a>
											<div class="messageDate" style="vertical-align: bottom; display: inline-block">
												<span><small>{{ message.createdDate }}</small></span>
											</div>
										</div>
									</b-list-group-item>
								</div>		
								
							</div>
						</div>				
						<!-- 메시지 목록 END -->
					</b-col>
				</b-row>
			
				<b-row>
					<b-col style="padding: 0;">
						<!-- 업로드 파일 미리보기 -->
						<div style="text-align: left; overflow-y: hidden; overflow-x:auto; white-space: nowrap; background-color: #F8F8FF;">
							<div v-for="(file, index) in uploadFiles" v-bind:key="index" style="display:inline-block; position:relative; margin-top:10px; padding:5px;">
								<div v-if="file.type.startsWith('image')" style="width:50px; height:50px; overflow:hidden;">
									<img :src="file.previewUrl" style="width:100%; height:100%; border-radius: 10px;">
								</div>
								<div v-else style="width:200px; height:50px; border-radius: 10px; background-color: #DCDCDC; padding:12px; display:inline-block; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">
									<b-icon icon="file-text-fill" scale="1.5" style="margin-right:10px;"></b-icon>
									<span>{{file.name}}</span>
								</div>
								<div v-on:click="deleteUploadFile(index)" type="button" style="position:absolute; top:-3px; right:-3px; background-color:#666666; width:20px; height:20px; color:white; text-align:center; font-weight: bold;">x</div>
							</div>
						</div>
						<!-- 업로드 파일 미리보기 END -->
					
						<!-- 메시지 입력 폼 -->
						<div class="input-group">
							<div class="input-group-prepend">
								<label class="input-group-text" for="uploadFileInput" type="button" >
									<b-icon id="uploadFileIcon" icon="upload" aria-hidden="true"></b-icon>
								</label>
								<input type="file" id="uploadFileInput" style="display:none;" v-on:change="addUploadFilesAndPreviewUrl" multiple>
							</div>
							<input type="input" class="form-control" v-model="textMessage" @keyup.enter="sendMessage('TEXT')">
							<div class="input-group-append">
								<button class="btn btn-primary" type="button" @click="sendMessage()">보내기</button>
							</div>
						</div>
						<!-- 메시지 입력 폼 END -->
					</b-col>
				</b-row>
			</b-col>
			
		</b-row>
	</b-container>
	
			
	</div>
</template>

<script>
/* eslint-disable no-unused-vars */
import api from "./BackendAPI";
import Stomp from 'webstomp-client';
import SockJS from 'sockjs-client';
import InfiniteLoading from "vue-infinite-loading";

/* let sock;
let ws; */
export default{
	name: "chatRoom",
	components: {
		InfiniteLoading,
	},
	data(){
		return{
			roomId: this.$route.params.roomId,
			chatParticipants: [],
			messages: [],
			textMessage: '',
			//stomp 연결
			sock: Object,
			stompClient: Object,
			isSubscribe: false,
			//파일 업로드
			uploadFiles: [],
			fileUrls: Array,
		}
	},
	methods:{
		//STOMP 연결.
		connectStomp: function(){
			return new Promise((resolve, reject) => {
				this.sock = new SockJS("/ws");
				this.stompClient = Stomp.over(this.sock);

				this.stompClient.connect({"Authorization" : "Bearer " + api.getCookieValue("access_token")}
					, (frame) => {
						//채팅방을 구독한다.
						this.subscribeChatRoom(this.stompClient);
						return resolve(this.stompClient);
						
					}, async (error) => {
						console.log(error);
						this.isSubscribe = false;
						// 인증 관련 예외처리
						if(error.reason === "UNAUTHORIZED"){
							await api.refresh()
							.then(res =>{
								console.log("WS connect 리프레시 성공");
							}).catch(error =>{
								console.log(error);
								alert("WS connect 리프레시 실패");
								//store 데이터를 초기화한다.
								localStorage.clear();
								location.href="/login";
							});
							
						// 다른 클라이언트에서 해당 채팅방 나간 경우.
						}else if(error.reason === "LEAVE"){
							return null;
						}
						//stomp 재연결 한다.
						return resolve(this.connectStomp());
					});
			});	
		},
		//채팅방을 구독한다.
		subscribeChatRoom(stompClient){
			return stompClient.subscribe("/topic/v1/chat/rooms/" + this.roomId, async (message) => {
				if(message.body){
					var recv = JSON.parse(message.body);
					// 구독 성공
					if(recv.type === 'SUBSCRIBE'){
						if(recv.sender === this.$store.getters.getMemberId){
							this.isSubscribe = true;
							// 참가자 목록 업데이트
							this.getParticipants();
							
							// 1.connect -> 2.subscribe -> 3.연결끊김 -> 4.reconnect -> 5.subscribe
							// 3과 5사이에 새로 생성된 메시지를 가져와야함.
							// 마지막으로 읽은 메시지를 가져온다.
							var lastReadMessage = await api.getLastReadMessage(this.roomId);
							var lastReadMessageDate = null;
							// 채팅방 처음 입장시 마지막으로 읽은 메시지 없음.
							if(lastReadMessage.data.messages.length > 0){
								lastReadMessageDate = lastReadMessage.data.messages[0].createdDate;
							}
							// 마지막으로 읽은 메시지 이후 목록을 가져와 추가한다.
							var messagesRes = await api.getMessages(this.roomId, lastReadMessageDate, "GOEDATE", null);
							messagesRes = messagesRes.data.messages;
							for(var i=0; i<messagesRes.length; i++){
								// 중복된 메시지를 제거한다. 
								if(this.messages.find(m => m.createdDate === messagesRes[i].createdDate)){
									continue;
								}
								this.messages.push(messagesRes[i]);
							}
							//메시지 리스트 날짜순으로 정렬
							this.messages.sort(this.compareMessageDate);
							
							
						}
					}else{
						this.recvMessage(recv);
						//입장하거나 나가는 사람이 있는 경우 참자가 정보를 갱신한다.
						if(recv.type === 'ENTER' || recv.type === 'LEAVE'){
							this.getParticipants();
						} 
					}
				}
			}, {"Authorization" : "Bearer " + api.getCookieValue("access_token"), "type" : "chatRoom"});
		},
		//stomp 연결을 종료한다.
		disconnectStomp(){
			this.stompClient.disconnect();
		},
		//메시지를 받는다.
		recvMessage: function(recv){
			this.messages.push(recv);
		},
		//메시지 목록을 가져온다.
		messageInfiniteHandler($state){
			//stompClient 구독 완료 후 기존 메시지 목록을 가져오도록 한다.
			if( !this.isSubscribe ){
				setTimeout( () => {
					$state.reset();
				}, 500);
			}else {
				let date = undefined;
				if(this.messages[0]) date = this.messages[0].createdDate; 
				let type = "LOEDATE";
				let size = 10;
				let firstMessageId;
				
				//채팅방 첫번째 메시지를 가져온다.
				api.getMessages(this.roomId, null, "GOEDATE", 1)
				.then(response =>{
					firstMessageId = response.data.messages[0].messageId;
				})
				
				api.getMessages(this.roomId, date, type, size)
				.then(response =>{
					setTimeout( () => {
						if(response.data.messages.length){
							var messagesRes = response.data.messages;
							for(var i=0; i<messagesRes.length; i++){
								// 중복된 메시지를 제거한다.
								// createdDate 기준 LOE로 메시지를 가져오기 때문에 중복된 메시지를 가져온다. 
								// LT로 가져오면 동일한 createdDate 메시지가 있을경우 메시지를 안 가져올수 있음. 
								if(this.messages.find(m => m.createdDate === messagesRes[i].createdDate)){
									continue;
								}
								this.messages.push(messagesRes[i]);
							}
							//메시지 리스트 날짜순으로 정렬
							this.messages.sort(this.compareMessageDate);
							$state.loaded();
						}
						//채팅방의 가장 첫번째 메시지일 경우 스크롤 완료. 
						if(firstMessageId === response.data.messages[0].messageId){
							$state.complete();
						}
					}, 100);
				});
			}
		},
		//메시지 날짜 비교
		compareMessageDate(a, b){
			var aDate = new Date(a.createdDate);
			var bDate = new Date(b.createdDate);
			if(aDate >= bDate) return 1
			else return -1
		},
		//채팅 방 참가자 목록을 가져온다.
		getParticipants: function(){
			api.getParticipants(this.roomId)
			.then(response => {
				this.chatParticipants = response.data.participants.slice();

			}).catch(error => {
				console.log(error);
			});
		},
		//로그인한 사용자가 채팅방에 존재하는지 확인한다.
		existsParticipant(){
			return api.existsParticipant(this.roomId);
		},
		//업로드 파일과 미리보기 url을 배열에 추가한다.
		addUploadFilesAndPreviewUrl(e){
			var files = Array.prototype.slice.call(e.target.files);
			for(var i=0; i<files.length; i++){
				var file = this.makePreviewUrl(files[i]);
				this.uploadFiles.push(file);
			}
			console.log(this.uploadFiles);
			//input file multiple은 중복파일 업로드가 되지 않기 때문에 input을 초기화한다.
			e.target.value = "";
		},
		//미리보기 url을 만든다.
		makePreviewUrl(file){
			/*
			FileReader를 사용해서 previewUrl에 base64로 인코딩된 파일 전체를 저장할 경우 즉각적으로 반응 안함.
			웹 페이지에 어떤 이벤트가 생겨야 previewUrl에 해당 파일의 정보가 저장됨.
			참조 : https://medium.com/@gabriele.cimato/on-how-to-store-an-image-in-redux-d623bcc06ca7	
			*/
			const localUrl =  window.URL.createObjectURL(file);
			file.previewUrl = localUrl;
			return file;
		},
		//업로드 파일을 삭제한다.
		deleteUploadFile(index){
			this.uploadFiles.splice(index, 1);
		},
		//메시지를 전송한다.
		sendMessage(){
			//텍스트 메시지가 빈 값이 아닐경우 텍스트 메시지 전송
			if(this.textMessage.replace(/\s/g, "") !== ""){
				api.sendMessage(this.roomId, "TEXT", this.textMessage)
				.then(response => {
				}).catch(error => {
					console.log(error);
				});
				this.textMessage = "";
			}
			
			//업로드 파일이 있으면 업로드 후 파일 메시지 전송
			for(var i=0; i<this.uploadFiles.length; i++){
				let type;
				if(this.uploadFiles[i].type.startsWith("image")) type = "IMAGE";
				else type = "FILE";
				
				this.uploadFile(this.uploadFiles[i])
				.then(response =>{
					api.sendMessage(this.roomId, type, response.data.url);
				}).catch(error =>{
					console.log(error);
				});
			}
			this.uploadFiles.splice(0, this.uploadFiles.length); //업로드 파일 배열 초기화
		},
		//파일을 업로드한다.
		uploadFile(file){
			var frm = new FormData();
			console.log(file);
			frm.append("uploadFile", file);
			frm.append("roomId", this.roomId);
			return api.uploadFile(frm);
		},
		//URL에서 파일 이름을 가져온다.
		getFileName(url){
			var splitUrl = url.split("/");
			var len = splitUrl.length;
			var fileName = splitUrl[len - 1].split("?")[0];
			return fileName;
		},
		//채팅방에서 나간다.
		leaveRoom(){
			api.leaveRoom(this.roomId)
			.then(response =>{
				this.stompClient.disconnect();
				this.$router.push({
					name: "Home",	
				})
			}).catch(error =>{
				console.log(error.response);
			})
		},
	},
	created: async function(){
		this.stompClient = await this.connectStomp(this);
		console.log(this.stompClient);
	},
	mounted() {
		// 새로고침시 offlineDate 업데이트 위해 stomp 연결끊기.
		window.addEventListener('beforeunload', this.disconnectStomp);
	},
	beforeUnmount() {
		window.removeEventListener('beforeunload', this.disconnectStomp);
	},
	beforeDestroy: function(){
		//페이지 이동시 웹소켓 연결을 종료한다.
		this.disconnectStomp();
	},
	watch:{
		async messages(val){
			var offsetHeight = this.$refs.messageWindow.offsetHeight;
			var scrollTop = this.$refs.messageWindow.scrollTop;
			var scrollHeight = this.$refs.messageWindow.scrollHeight;
			
			await this.$nextTick(); //DOM 업데이트 완료 후 수행하도록 한다.
			//스크롤이 제일 하단일 경우 스크롤을 하단으로 이동시킨다. 모바일 고려해서 +200 해줌.
			if (offsetHeight + scrollTop + 200 >= scrollHeight) {
				this.$refs.messageWindow.scrollTop = this.$refs.messageWindow.scrollHeight;
			}
		}
	}
}
</script>
