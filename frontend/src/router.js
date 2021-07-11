/* eslint-disable no-unused-vars */
import Vue from 'vue'
import Router from 'vue-router'
import Signup from '@/components/Signup'
import Login from '@/components/Login'
import Profile from '@/components/Profile'
import ChatRooms from '@/components/ChatRooms'
import ChatRoom from '@/components/ChatRoom'
import ChatRoomCreation from '@/components/ChatRoomCreation'
import store from './store'

Vue.use(Router);

const router = new Router({
	mode: 'history',
	routes: [
		{path : '/', component : ChatRooms, name: "Home"},
		{path : '/signup', component : Signup},
		{path : '/login', component : Login},
		{path : '/profile', component : Profile},
		{path : '/chat/rooms', component : ChatRooms, name : "ChatRooms", props: true},
		{path : '/chat/rooms/creation', component : ChatRoomCreation},
		{path : '/chat/rooms/:roomId', component : ChatRoom, name: "ChatRoom"},
		// otherwise redirect to home
        {path: '*', redirect: '/' }
	]
});

export default router;
