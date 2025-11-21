import { createRouter,createWebHashHistory } from 'vue-router';

const router = createRouter({
    history:createWebHashHistory(),
    routes:[
        {path:'/',component:()=>import('./views/Main.vue')},
        {path:'/user/login',component:()=>import('./views/Login.vue')},
        {path:'/user/register',component:()=>import('./views/Register.vue')}
    ]
});

export default router;