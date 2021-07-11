<template>
	<div id="signup" class="container">
		<div style="text-align: center;">
			<div style="display: inline-block;">
				<div class="card" style="width:22rem; border-radius:20px;">
					<div class="card-body">
						<h4 style="margin-bottom: 40px; opacity: 0.7;">회원 정보를 입력하세요.</h4>
						<input type="text" v-model="memberId" class="signupInfo form-control" placeholder="ID" required autofocus>
						<div v-if="memberIdErrorMessage != ''" class="signupErrorMessage">
							{{ memberIdErrorMessage }}
						</div>
						
						<input type="password" v-model="password" class="signupInfo form-control" placeholder="Password" required>
						<div v-if="passwordErrorMessage != ''" class="signupErrorMessage">
							{{ passwordErrorMessage }}
						</div>
						
						<input type="text" v-model="name" class="signupInfo form-control" placeholder="Name" required>
						<div v-if="nameErrorMessage != ''" class="signupErrorMessage">
							{{ nameErrorMessage }}
						</div>
						
						<input type="text" v-model="email" class="signupInfo form-control" placeholder="Email" required>
						<div v-if="emailErrorMessage != ''" class="signupErrorMessage">
							{{ emailErrorMessage }}
						</div>
						
						<button v-on:click="signup();" class="btn btn-lg btn-primary btn-block">회원가입</button>
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
	name: 'signup',
	data(){
		return {
			memberId: '',
			password: '',
			name: '',
			email: '',
			memberIdErrorMessage: '',
			passwordErrorMessage: '',
			nameErrorMessage: '',
			emailErrorMessage: '',
		}
	},
	methods:{
		signup: function(){
			api.signup(this.memberId, this.password, this.name, this.email)
				.then(response =>{
					alert("이메일 인증을 해주세요.");
					location.href="/login";
				}).catch(error =>{
					console.log("회원가입 실패");
					//메시지 초기화
					this.memberIdErrorMessage = '';
					this.passwordErrorMessage = '';
					this.nameErrorMessage = '';
					this.emailErrorMessage = '';
					
					//유효성 검사
					if(error.response.data.code === 'INVALID_INPUT_VALUE'){	
						var errors = error.response.data.errors;
						for(var i = 0; i < errors.length; i++){
							if(errors[i].field === 'memberId') this.memberIdErrorMessage = errors[i].reason;
							else if(errors[i].field === 'password') this.passwordErrorMessage = errors[i].reason;
							else if(errors[i].field === 'name') this.nameErrorMessage = errors[i].reason;
							else if(errors[i].field === 'email') this.emailErrorMessage = errors[i].reason;
						}
					//아이디 중복
					}else if(error.response.data.code === 'ACCOUNT_DUPLICATION'){
						this.memberIdErrorMessage = error.response.data.message;
					}
				})
		}
	}
}
</script>

<style>
.signupInfo {
	margin-bottom: 15px;
}
.signupErrorMessage {
	text-align: left;
	margin-bottom: 5px;
}
</style>
