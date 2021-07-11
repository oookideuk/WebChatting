/* eslint-disable no-useless-escape, no-unused-vars, no-constant-condition */
import axios from 'axios'

const AXIOS = axios.create({
  timeout: 6000
});

export default{
	//회원가입
	signup(memberId, password, name, email){
		let params = new URLSearchParams();
		params.append('memberId', memberId);
		params.append('password', password);
		params.append('name', name);
		params.append('email', email);
		return AXIOS.post('/v1/members', params);
	},
	//로그인
	login(memberId, password){
		let params = new URLSearchParams();
		params.append('memberId', memberId);
		params.append('password', password);
		return AXIOS.post('/v1/auth/login', params);
	},
	//로그아웃
	logout(){
		return AXIOS.post('/v1/auth/logout');
	},
	//사용자 인증
	authMe(){
		return AXIOS.get('/v1/auth/me');
	},
	//리프레시
	refresh(){
		return AXIOS.post("/v1/auth/refresh");
	},
	//회원 정보를 가져온다.
	getMemberInfo(memberId){
		return AXIOS.get('/v1/members/' + memberId);
	},
	//프로필 사진 가져오기
	getProfilePicture(memberId){
		return AXIOS.get('/v1/profilePicture/' + memberId);
	},
	//프로필 사진 등록하기
	registerProfilePicture(formData){
		return AXIOS.post('/v1/profilePicture', formData, {
			headers: {
				'Content-Type': 'multipart/form-data'
			}
		});
	},
	//프로필 사진 삭제하기
	deleteProfilePicture(){
		return AXIOS.delete('/v1/profilePicture');
	},
	//로그인한 사용자가 채팅방에 참가했는지 확인한다.
	existsParticipant(roomId){
		let params = new URLSearchParams();
		params.append("roomId", roomId);
		return AXIOS.get("/v1/chat/rooms/" + roomId + "/participant/exist");
	},
	//채팅 방 리스트 검색
	searchRooms(searchType, keyword, page, size, sortType, direction){
		let params = new URLSearchParams();
		params.append("searchType", searchType);
		params.append("keyword", keyword);
		params.append("page", page);
		params.append("size", size);
		params.append("sort", sortType+","+direction);
		return AXIOS.get("/v1/chat/rooms", {params});
	},
	//채팅 방 입장
	enterRoom(roomId){
		location.href="/chat/rooms/"+roomId;
	},
	//채팅 방 나가기
	leaveRoom(roomId){
		return AXIOS.post("/v1/chat/rooms/" + roomId + "/leave");
	},
	//채팅 방 생성
	createRoom(title){
		let params = new URLSearchParams();
		params.append("title", title)
		return AXIOS.post("/v1/chat/rooms", params);
	},
	//채팅 방 참가자 목록을 가져온다.
	getParticipants(roomId){
		return AXIOS.get("/v1/chat/rooms/" + roomId + "/participants");
	},
	//채팅 방 메시지 목록을 가져온다.
	getMessages(roomId, date, type, size){
		let params = new URLSearchParams();
		if(type) params.append("type", type);
		if(date) params.append("date", date);
		if(size) params.append("size", size);
		
		return AXIOS.get("/v1/chat/rooms/" + roomId + "/messages", {params});
	},
	// 마지막으로 읽은 메시지를 가져온다.
	getLastReadMessage(roomId){
		return AXIOS.get("/v1/chat/rooms/" + roomId + "/messages/lastRead");
	},
	//파일을 업로드한다.
	uploadFile(formData){
		return AXIOS.post("/v1/chat/upload", formData, {
			headers: {
				"Content-Type" : "multipart/form-data"
			}
		});
	},
	//메시지를 전송한다.
	sendMessage(roomId, type, message){
		let params = new URLSearchParams();
		params.append('roomId', roomId);
		params.append('type', type);
		params.append('message', message);
          
		return AXIOS.post('/v1/chat/rooms/'+roomId+'/messages/send', params);
	},
	//주어진 이름의 쿠키를 반환하는데, 조건에 맞는 쿠키가 없다면 undefined를 반환한다.
	getCookieValue(name) {
		let matches = document.cookie.match(new RegExp(
				"(?:^|; )" + name.replace(/([\.$?*|{}\(\)\[\]\\\/\+^])/g, '\\$1') + "=([^;]*)"
		));
		return matches ? decodeURIComponent(matches[1]) : undefined;
	},
}

//주어진 이름의 쿠키를 반환하는데, 조건에 맞는 쿠키가 없다면 undefined를 반환한다.
function getCookieValue(name) {
	let matches = document.cookie.match(new RegExp(
			"(?:^|; )" + name.replace(/([\.$?*|{}\(\)\[\]\\\/\+^])/g, '\\$1') + "=([^;]*)"
	));
	return matches ? decodeURIComponent(matches[1]) : undefined;
}

//request 인터셉터
AXIOS.interceptors.request.use(
	function(config){
		let access_token = getCookieValue("access_token");
		config.headers.Authorization = "Bearer " + access_token;
		return config;
	},
	function (error){
		return Promise.reject(error);
	}
)

//response 인터셉터
AXIOS.interceptors.response.use(
	function (response) {
		return response;
	},
	function (error) {
		if(error.response){
			if(error.response.data.code == "UNAUTHORIZED" && !error.config._isRetry){
				/* 
				 * 무한루프를 방지한다.
				 * _isRetry를 주석 처리하고 Authorization header를 생략하면 무한루프 발생한다. 
				 */
				error.config._isRetry = true;
			
				console.log("로그인이 필요합니다.");
				return axios.post("/v1/auth/refresh")
					.then(response => {
						console.log("리프레시 성공");
						//request 인터셉터를 타지 않기 때문에 리프레시한 액세스 토큰으로 변경한다.
						let access_token = getCookieValue("access_token");
						error.config.headers.Authorization = "Bearer " + access_token;
					
						return axios(error.config);
					}).catch(error => {
						console.log(error)
					
						//로그인 관련 store 데이터를 초기화한다.
						localStorage.clear();
					
						//리프레시 실패시 로그인 화면으로 이동한다.
						if(error.response.data.code == "REFRESH_FAILURE"){
							location.href="/login";
						}
						return Promise.reject(error);
					});
			}
		}
		return Promise.reject(error);
	}
)