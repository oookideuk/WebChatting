<template>
	<div class="container" id="chatRoomCreation">
		<div style="margin-bottom: 50px; opacity: 0.7;">
			<h4>채팅방 이름을 입력하세요.</h4>
		</div>
		
		<input id="roomTitle" type="text" v-model="roomTitle" @keyup.enter="createRoom">
		<button class="btn btn-primary mb-2" @click="createRoom">만들기</button>
	</div>
</template>

<script>
/* eslint-disable no-unused-vars */
import api from "./BackendAPI";
export default {
	name: "chatRoomCreation",
	data(){
		return{
			roomTitle: '',
		}
	},
	methods: {
		createRoom: function(){
			console.log(this.roomTitle);
			api.createRoom(this.roomTitle)
				.then(response =>{
					//생성한 채팅방으로 이동한다.
					api.enterRoom(response.data.roomId);
					
				}).catch(error =>{
					console.log("채팅방 개설에 실패했습니다.");
					console.log(error.response);
				});
		}
	}
}
</script>

<style>
#roomTitle {
  background: transparent;
  border: none;
  border-bottom: 1px solid #000000;
  -webkit-box-shadow: none;
  box-shadow: none;
  border-radius: 0;
  width: 70%;
  margin-right: 10px;
}

#roomTitle:focus {
  -webkit-box-shadow: none;
  box-shadow: none;
}
</style>
