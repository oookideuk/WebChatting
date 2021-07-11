<template>
  <div class="container" id="app">
    <div id="header">
      <main-header/>
    </div>
    <!-- <router-view :key="$route.fullPath"></router-view> -->
    <router-view :key="$route.hash"></router-view>
  </div>
</template>

<script>
/* eslint-disable no-unused-vars */
import MainHeader from './components/MainHeader'
export default {
	name: 'app',
	components:{
		'main-header' : MainHeader
	},
}
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
  margin-top: 20px;
}
</style>
