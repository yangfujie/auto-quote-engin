<template>
  <div id="app">
    <el-menu mode="horizontal" router>
      <el-menu-item index="/strategies">策略管理</el-menu-item>
      <el-menu-item index="/instances">实例管理</el-menu-item>
      <el-menu-item index="/monitor">运行监控</el-menu-item>
    </el-menu>
    <router-view />
  </div>
</template>