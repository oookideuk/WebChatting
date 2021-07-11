/* eslint-disable no-unused-vars */
import Vue from 'vue'
import App from './App.vue'
import axios from 'axios'
import {BootstrapVue, BootstrapVueIcons} from 'bootstrap-vue'
import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap-vue/dist/bootstrap-vue.css'
import store from './store'
import router from './router'
import VueMoment from 'vue-moment'

Vue.prototype.$axios = axios;
Vue.use(BootstrapVue);
Vue.use(BootstrapVueIcons);
Vue.use(VueMoment);
Vue.config.productionTip = false;

export const eventBus = new Vue();

new Vue({
	router,
	store,
	render: h => h(App),
	data:{
	},
	methods:{
	},
}).$mount('#app')
