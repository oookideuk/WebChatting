<template>
	<div class="container" id="profile" v-cloak>
		<div style="text-align: center;">
			<div style="display: inline-block;">
				<div class="card" style="width:22rem; border-radius:20px;">
					<div class="card-body">
						<div style="text-align: left; margin-bottom: 20px;">
							<input type="file" name="file" id="profilePictureInput" style="display: none;" v-on:change="previewProfilePicture">
							<label for="profilePictureInput">
								<img id="profilePictureImg" :src="profilePictureUrl" type="button" width="150" height="150" alt="프로필" @error="replaceByDefaultImg">
							</label>
							
							<span style="vertical-align: top; margin-left: 15px;">
								<span v-if="profilePictureUrl === ''" type="button" v-on:click="registerProfilePicture()" style="font-weight: 100;">등록</span>
								<span v-else>
									<span type="button" v-on:click="deleteProfilePicture()" style="font-weight: 100;">삭제</span>
								</span>
							</span>
						</div>
						<div style="text-align: left;">
							<div class="form-group">
								<label for="name" style="font-weight: 550;">이름</label>
								<span class="form-control" id="name">{{ name }}</span>
							</div>
							<div class="form-group">
								<label for="email" style="font-weight: 550;">이메일</label>
								<span class="form-control" id="email">{{ email }}</span>
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
</template>

<script>
/* eslint-disable no-unused-vars */
import api from "./BackendAPI";
export default {
	name: "profile",
	data(){
		return{
			name: '',
			email: '',
			profilePictureUrl: this.$store.getters.getProfilePictureUrl,
		}
	},
	methods:{
		//프로필 사진이 없을 경우 기본 사진으로 설정한다.
		replaceByDefaultImg(e){
			e.target.src = require('@/assets/defaultProfilePicture.png');
		},
		//프로필 사진 미리보기
		previewProfilePicture(e){
			var files = e.target.files;
			var fileArr = Array.prototype.slice.call(files);
			
			// 사진 선택 전 취소한 상태.
			if(fileArr.length == 0){
				// 미리보기 초기화
				document.getElementById("profilePictureImg").setAttribute("src", "");
			}
			
			fileArr.forEach(function(f) {
				if(!f.type.match("image.*")){
					document.getElementById("profilePictureInput").value = ''; // input 초기화
					alert("이미지 파일만 등록 가능합니다.");
					return;
				}
				
				var reader = new FileReader();
				reader.onload = function(e){
					document.getElementById("profilePictureImg").setAttribute("src", e.target.result);
				}
				reader.readAsDataURL(f);
			});
		},
		//프로필 사진 등록
		registerProfilePicture(){
			var frm = new FormData();
			var file = document.getElementById("profilePictureInput");
			
			if(file.files.length > 0){
				frm.append("file", file.files[0]);
				api.registerProfilePicture(frm)
					.then(response =>{
						//등록한 프로필 사진으로 store 갱신하기.
						this.$store.dispatch("changeProfilePicture", {profilePictureUrl : response.data.src});
					}).catch(e =>{
						console.log(e.response);
						if(e.response.data.code === 'EXCEED_PROFILE_PICTURE'){
							alert(e.response.data.message);
						}else{
							alert("사진 등록에 실패했습니다.");	
						}
					});
				
			}else{
				alert("사진을 선택하세요.")
			}
		},
		//프로필 사진 삭제
		deleteProfilePicture(){
			// 중복파일 선택이 되지 않기 때문에 input을 초기화한다.
			// ex) 사진 A 등록된 상황 -> 사진 B 선택 -> 사진 A 삭제 -> 사진 B 선택 -> on change 안됨.
			document.getElementById("profilePictureInput").value = '';
			api.deleteProfilePicture()
				.then(response => {
					//store 갱신
					this.$store.dispatch("changeProfilePicture", {profilePictureUrl : ''});
				}).catch(error => {
					console.log(error.response);
				});
		}
	},
	created: function(){
		//회원 정보 가져오기
		api.getMemberInfo(this.$store.getters.getMemberId)
			.then(response =>{
				this.name = response.data.name;
				this.email = response.data.email;
			}).catch(error =>{
				console.log(error.response);
			});
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