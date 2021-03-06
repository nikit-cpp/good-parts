// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import VueResource from 'vue-resource'
import App from './App.vue'
import router from './router.js'
import Notifications from './notifications'
import store from './store'
import {UNSET_USER, FETCH_USER_PROFILE} from './store'
import {LOGIN_MODAL, PROFILE_URL} from './constants'
import bus from './bus'
import {UNAUTHORIZED} from './bus'
import Meta from 'vue-meta'
import VueProgressBar from 'vue-progressbar'

Vue.use(VueResource);
Vue.use(Meta);
Vue.use(VueProgressBar, {
    color: '#29d',
    failedColor: 'red',
    height: '2px'
});

// Vue.config.devtools = false;

function getCookie(name) {
    const value = "; " + document.cookie;
    const parts = value.split("; " + name + "=");
    if (parts.length === 2) return parts.pop().split(";").shift();
}

Vue.http.interceptors.push((request, next) => {

    // https://docs.spring.io/spring-security/site/docs/current/reference/html/csrf.html#csrf-cookie
    const csrfCookieValue = getCookie('XSRF-TOKEN');
    // console.log('csrfCookieValue', csrfCookieValue);
    request.headers.set('X-XSRF-TOKEN', csrfCookieValue);

    next((response) => {
        if(response.status === 401) {
            store.commit(UNSET_USER);
            bus.$emit(UNAUTHORIZED, null);

            // we show modal always except on immediate get profile
            if (request.url!==PROFILE_URL) {
                vm.$modal.show(LOGIN_MODAL);
            }
        } else if (response.status === 400) {
            console.debug("Bad request", response);
        } else if (!(response.status >= 200 && response.status < 300 || response.status === 404)) {
            console.error("Unexpected error", response);
            Notifications.unexpectedError(request.method, request.url, response.status);
        }
    });
});

window.addEventListener("load", function(){
    console.log("On load");
    store.dispatch(FETCH_USER_PROFILE);
});

/* eslint-disable no-new */
const vm = new Vue({
  el: '#app-container',
  router,
  template: '<App/>',
  components: { App }
});

