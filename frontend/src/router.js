import { createRouter,createWebHashHistory } from 'vue-router';
import UserEdit from '@/views/UserEdit.vue'

const router = createRouter({
    history:createWebHashHistory(),
    routes:[
        {path:'/',component:()=>import('./views/Main.vue')},
        {path:'/user/login',component:()=>import('./views/Login.vue')},
        {path:'/user/register',component:()=>import('./views/Register.vue')},
        {path:'/chat/:characterId',
            name:'Chat'
            ,component:()=>import('./views/Chat.vue')
            ,props:true
            ,beforeEnter:(to) => {
                if(!to.params.characterId){
                    return {path:'/'};
                }
            }
        },
        {
            path:'/user/edit',
            name:'UserEdit',
            component: UserEdit
        }
    ]
});

export default router;