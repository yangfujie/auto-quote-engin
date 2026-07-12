import { createRouter, createWebHistory } from 'vue-router'
import StrategyList from '../views/StrategyList.vue'
import StrategyEditor from '../views/StrategyEditor.vue'
import InstanceManage from '../views/InstanceManage.vue'
import Monitor from '../views/Monitor.vue'

const routes = [
    { path: '/', redirect: '/strategies' },
    { path: '/strategies', component: StrategyList },
    { path: '/editor/:id?', component: StrategyEditor },
    { path: '/instances', component: InstanceManage },
    { path: '/monitor', component: Monitor }
]

export default createRouter({
    history: createWebHistory(),
    routes
})