<template>
  <div>
    <el-button type="primary" @click="$router.push('/editor')">新建策略</el-button>
    <el-table :data="strategyDefs" style="margin-top:20px">
      <el-table-column prop="id" label="ID" />
      <el-table-column prop="name" label="策略名称" />
      <el-table-column prop="createTime" label="创建时间" />
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button size="mini" @click="$router.push(`/editor/${row.id}`)">编辑</el-button>
          <el-button size="mini" type="success" @click="createInstance(row.id)">创建实例</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
<script>
import { ref, onMounted } from 'vue'
import { strategyApi } from '../api'
import { useRouter } from 'vue-router'

export default {
  setup() {
    const strategyDefs = ref([])
    const router = useRouter()

    const load = async () => {
      const res = await strategyApi.listDef()
      strategyDefs.value = res.data
    }
    const createInstance = (defId) => {
      // 跳转到实例创建页，带defId
      router.push(`/instances?defId=${defId}`)
    }
    onMounted(load)
    return { strategyDefs, createInstance }
  }
}
</script>