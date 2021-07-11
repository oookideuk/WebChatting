<template>
	<div id="login" class="container">
		<div style="text-align: center;">
			<div style="display: inline-block;">
				<div class="card" style="width:22rem; border-radius:20px;">
					<div class="card-body">
						<h4 style="margin-bottom: 40px; opacity: 0.7;">로그인 정보를 입력하세요.</h4>
						<input @keyup.enter="login();" type="text" v-model="memberId" class="loginInfo form-control" placeholder="ID" required autofocus>
						<div v-if="memberIdErrorMessage != ''" class="loginErrorMessage">
							{{ memberIdErrorMessage }}
						</div>
						
						<input @keyup.enter="login();" type="password" v-model="password" class="loginInfo form-control" placeholder="Password" required>
						<div v-if="passwordErrorMessage != ''" class="loginErrorMessage">
							{{ passwordErrorMessage }}
						</div>
						
						<button v-on:click="login();" class="btn btn-lg btn-primary btn-block">로그인</button>
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
	name: 'login',
	data(){
		return {
			memberId : '',
			password : '',
			memberIdErrorMessage : '',
			passwordErrorMessage : '',
		}
	},
	methods:{
		login: function(){
			this.$store.dispatch("login", {memberId: this.memberId, password: this.password})
				.then(response => {
					this.$router.push("/");
				}).catch(error => {
					console.log(error.response);
					
					//메시지 초기화
					this.memberIdErrorMessage = '';
					this.passwordErrorMessage = '';
					
					//잘못된 아이디 or 패스워드
					if(error.response.data.code === 'INVALID_LOGIN_INPUT'){
						this.passwordErrorMessage = error.response.data.message;
					//유효성 검사
					}else if(error.response.data.code === "INVALID_INPUT_VALUE"){
						var errors = error.response.data.errors;
						for(var i = 0; i < errors.length; i++){
							if(errors[i].field === 'memberId') this.memberIdErrorMessage = errors[i].reason;
							else if(errors[i].field === 'password') this.passwordErrorMessage = errors[i].reason;
						}
					}
				});
		}
	}
}
</script>

<style>
.loginInfo {
	margin-bottom: 15px;
}
.loginErrorMessage {
	text-align: left;
	margin-bottom: 5px;
}
</style>
