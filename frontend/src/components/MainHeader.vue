<template>
	<div style="margin-bottom: 50px;">
		<b-navbar toggleable="lg" type="dark" variant="dark">
		<!-- <nav class="mb-1 navbar navbar-expand-lg navbar-dark info-color"> -->
			<b-navbar-brand type="button" v-on:click="goHome()">Home</b-navbar-brand>
			<b-navbar-toggle target="nav-collapse"></b-navbar-toggle>
			
			<b-collapse id="nav-collapse" is-nav>
				<!-- Right aligned nav items -->
				<b-navbar-nav class="ml-auto">
					<!-- 채팅 방 목록 검색 -->
					<div class="form-inline">
						<b-form-input type="search" v-model="keyword" @keyup.enter="goChatRooms" size="sm" class="mr-sm-2" placeholder="Search..."></b-form-input>
					</div>
					
					<div style="text-align: right;">
						<!-- 로그인 상태 -->
						<b-nav-item-dropdown v-if="this.$store.getters.isLoggedin">
							<template #button-content>
								<img :src="profilePictureUrl" height="40" width="50" alt="프로필" @error="replaceByDefaultImg">
							</template>
							<b-dropdown-item>
								<router-link tag="div" to="/profile">프로필</router-link>
							</b-dropdown-item>
							<b-dropdown-item v-on:click="logout()">로그아웃</b-dropdown-item>
						</b-nav-item-dropdown>
						<!-- 로그아웃 상태 -->
						<b-navbar-nav v-else>
							<b-nav-item>
								<router-link tag="span" to="/signup">회원가입</router-link>
							</b-nav-item>
							<b-nav-item>
								<router-link tag="span" to="/login">로그인</router-link>
							</b-nav-item>
						</b-navbar-nav>
					</div>
					
				</b-navbar-nav>
			</b-collapse>
		</b-navbar>
	</div>
</template>


<script>
/* eslint-disable no-unused-vars */
import api from "./BackendAPI";
export default {
	name: 'main-header',
	data(){
		return {
			profilePictureUrl: this.$store.getters.getProfilePictureUrl,
			keyword: '',
		}
	},
	methods:{
		//로그아웃
		logout: function(){
			this.$store.dispatch("logout")
				.then(response =>{
					console.log(response);
					location.href="/login";
				}).catch(error =>{
					console.log(error.response);
				});
		},
		//프로필 사진이 없을 경우 기본 사진으로 설정한다.
		replaceByDefaultImg(e){
			e.target.src = require('@/assets/defaultProfilePicture.png');
		},
		//기본 화면으로 이동한다.
		goHome: function(){
			this.$router.push({
				name: "Home",
			}).catch(error =>{
				console.log(error.name);
				if(error.name === 'NavigationDuplicated'){
					this.$router.go();																																																																																						
				}
			});
		},
		//채팅방 리스트 화면으로 이동한다.
		goChatRooms: function(){
			this.$router.push({
				name: 'ChatRooms',
				params: {
					pKeyword: this.keyword,
				},
				hash: Math.floor(Math.random() * 100)
			}).catch(error =>{
				console.log(error.name);
			});
		},
	},
	computed:{
		checkProfilePictureUrl(){
			return this.$store.getters.getProfilePictureUrl;
		}
	},
	watch:{
		checkProfilePictureUrl(val){
			this.profilePictureUrl = val;
		}
	}
}
</script>

