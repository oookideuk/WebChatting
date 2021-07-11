/* eslint-disable no-unused-vars */
import Vue from 'vue'
import Vuex from 'vuex'
import api from './components/BackendAPI'
import createPersistedState from 'vuex-persistedstate'

Vue.use(Vuex);

export default new Vuex.Store({
	plugins: [
		createPersistedState()
	],
	state:{
		isLoggedin : false,
		memberId : '',
		profilePictureUrl : '',
	},
	mutations:{
		login_success(state, payload){
			state.isLoggedin = true;
			state.memberId = payload.memberId;
			state.profilePictureUrl = payload.profilePictureUrl;
		},
		logout_success(state){
			state.isLoggedin = false;
			state.memberId = '';
			state.profilePictureUrl = '';
		},
		changeProfilePicture(state, payload){
			state.profilePictureUrl = payload.profilePictureUrl;
		}
	},
	actions:{
		//로그인
		login({commit}, {memberId, password}){
			return new Promise((resolve, reject) =>{
				api.login(memberId, password)
					.then(response =>{
						//프로필 사진을 세팅한다.
						api.getProfilePicture(memberId)
							.then(response => {
								commit('login_success', {
									memberId : memberId,
									profilePictureUrl : response.data.src
								});
							}).catch(error =>{
								commit('login_success', {
									memberId : memberId,
									profilePictureUrl : ''
								});
							})

						resolve(response);
					}).catch(error =>{
						console.log(error.response);
						reject(error);
					});
			});
		},
		//로그아웃
		logout({commit}){
			return new Promise((resolve, reject) =>{
				api.logout()
					.then(response =>{
						commit('logout_success');
						resolve(response);
					}).catch(error =>{
						reject(error);
					});
			});
		},
		//프로필 사진 교체
		changeProfilePicture({commit}, {profilePictureUrl}){
			return new Promise((resolve, reject) =>{
				commit('changeProfilePicture', {
					profilePictureUrl : profilePictureUrl,
				});
			});
		}
	},
	getters:{
		isLoggedin: state => state.isLoggedin,
		getMemberId: state => state.memberId,
		getProfilePictureUrl: state => state.profilePictureUrl,
	}
})