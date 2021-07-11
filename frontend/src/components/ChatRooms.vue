<!-- 실시간 업데이트 X -->
<template>
	<div class="container" id="chatRooms" v-cloak>
		<div style="margin: 0 0 10px 10px; float: left;">
			<!-- 채팅 방 생성 페이지로 이동한다. -->
			<a href="#" v-on:click="selectTotalRoom()"><span style="margin-right: 10px;">전체</span></a>
			<span style="margin-right: 10px;">/</span>
			<a href="#" v-on:click="selectParticipatingRoom()"><span style="margin-right: 10px;">참가중</span></a>
		</div>
			
		<div style="float:right; margin-right: 10px;">
			<!-- 채팅 방 생성 페이지로 이동한다. -->
			<router-link to="/chat/rooms/creation">
				<span>new chat</span>
			</router-link>
		</div>	
		
		<div class="chatList" style="clear:both;">
			<!-- 참가중인 채팅방 -->
			<ul v-if="searchType === 'PARTICIPANT'" id="participatingRoom" class="list-group">
				<li class="list-group-item list-group-item-action" v-for="room in rooms" v-bind:key="room.roomId" v-on:click="enterRoom(room.roomId)">
					<span class="title" style="float: left; text-align: left; width:70%; font-weight: 500; display:inline-block; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">{{ room.title }}</span>
					<span class="lastMessageDate" style="float: right">{{ $moment(room.lastMessageDate).format('YYYY-MM-DD') }}</span> <br>
					<span v-if = "room.lastMessageType === 'IMAGE' || room.lastMessageType === 'FILE'" class="lastMessage" style="opacity: 0.7; float: left; text-align: left; width:70%; display:inline-block; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">바이너리 파일입니다.</span>
					<span v-else class="lastMessage" style="opacity: 0.7; float: left; text-align: left; width:70%; display:inline-block; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">{{ room.lastMessage }}</span>
					<span v-if = "room.unreadMessageCount > 0" class="unreadMessageCount badge badge-danger badge-pill" style="float: right; margin-top: 5px;">{{ room.unreadMessageCount }}</span>
				</li>
			</ul>
			
			<!-- 전체 채팅방 -->
			<ul v-else id="totalRoom" class="list-group">
				<li class="list-group-item list-group-item-action" v-for="room in rooms" v-bind:key="room.roomId" v-on:click="enterRoom(room.roomId)" style="padding:25px 20px 25px 20px;">
					<span class="title" style="float: left; text-align: left; width:70%; font-weight: 500; display:inline-block; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">{{ room.title }}</span>
					<span class="lastMessageDate" style="float: right">{{ $moment(room.lastMessageDate).format('YYYY-MM-DD') }}</span> <br>
					
					<!-- <span v-if = "room.lastMessageType === 'IMAGE' || room.lastMessageType === 'FILE'" class="lastMessage" style="opacity: 0.7; float: left; text-align: left; width:70%; display:inline-block; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">바이너리 파일입니다.</span> -->
					<!-- <span v-else class="lastMessage" style="opacity: 0.7; float: left; text-align: left; width:70%; display:inline-block; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">{{ room.lastMessage }}</span> -->
				</li>
			</ul>
			
			<!-- 채팅 방 리스트 스크롤 페이징 적용 -->
			<infinite-loading @infinite="infiniteHandler" spinner="waveDots">
				<div slot="no-more" style="color: rgb(102, 102, 102); font-size: 14px; padding: 10px 0px;">목록의 끝입니다.</div>
			</infinite-loading>
		</div>
	</div>
</template>

<script>
/* eslint-disable no-unused-vars */
import api from "./BackendAPI";
import InfiniteLoading from "vue-infinite-loading";
export default {
	name: "chatRooms",
	components: {
		InfiniteLoading
	},
	props : {
		pSearchType: {
			type: String, default: 'TITLE'
		},
		pKeyword: {
			type: String, default: ''
		},
	},
	data(){
		return {
			rooms : [],
			searchType : this.pSearchType,
			keyword : this.pKeyword,
			page : 1,
			size : 50,
			sortType : 'TITLE', //'MESSAGE',	//'CREATED_DATE',
			direction : 'DESC',
			scrollState: Object,
		}
	},
	methods:{
		// 채팅 방에 입장한다.
		enterRoom: function(roomId){
			//api.enterRoom(roomId);
			this.$router.push({
				name: "ChatRoom",
				params: { roomId : roomId }
			})
		},
		// 전체 채팅방
		selectTotalRoom(){
			this.searchType = "TITLE";	// 제목 검색
			this.keyword = "";
			this.page = 1;
			this.sortType = "TITLE";	// 제목 기준으로 정렬
			this.rooms = [];
			this.scrollState.reset();	// scroll 상태 초기화
		},
		// 참가중 채팅방
		selectParticipatingRoom(){
			this.searchType = "PARTICIPANT";	// 참가중인 채팅방 검색
			this.keyword = "";
			this.page = 1;
			this.sortType = "MESSAGE";	// 최신 메시지 생성일 기준으로 정렬
			this.rooms = [];
			this.scrollState.reset();	// scroll 상태 초기화
		},
		// 채팅 방 리스트를 가져온다.
		infiniteHandler: function($state){
			this.scrollState = $state;
			api.searchRooms(this.searchType, this.keyword, this.page, this.size, this.sortType, this.direction)
				.then(response =>{
					setTimeout( () => {
						//더 이상 불러올 데이터가 없을 때 사용한다.
						//무한 스크롤 작업 수행하지 않음.
						if(response.data.rooms.length < this.size){ 
							$state.complete(); 
						}
						
						response.data.rooms.forEach(room => this.rooms.push(room));
						//데이터 로드가 전부 수행되었다는 것을 알려준다.
						//다음 request가 있을 때까지 대기 상태로 들어간다.
						$state.loaded();
						this.page += 1;	//다음 page
						//console.log("next page : " + this.page);
						//console.log("rooms total size : " + this.rooms.length );
					}, 100);
					
				}).catch(error =>{
					console.error(error);
				});
		}
	}
}
</script>