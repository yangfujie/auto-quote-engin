<template>
  <div>
    <el-form :model="form" label-width="120px">
      <el-form-item label="策略定义">
        <el-select v-model="form.strategyDefId">
          <el-option v-for="d in defs" :key="d.id" :label="d.name" :value="d.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="标的">
        <el-input v-model="form.symbol" />
      </el-form-item>
      <el-form-item label="优先级">
        <el-input-number v-model="form.priority" :min="1" :max="10" />
      </el-form-item>
      <el-form-item label="触发条件">
        <el-input v-model="form.triggerConditions" type="textarea" rows="3" placeholder='{"type":"AND","conditions":[{"field":"price","operator":">","value":100}]}' />
      </el-form-item>
      <el-form-item label="参数覆盖">
        <el-input v-model="form.params" type="textarea" rows="3" placeholder='{"multiplier":"0.98"}' />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submit">创建实例</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="instances">
      <el-table-column prop="id" label="ID" />
      <el-table-column prop="symbol" label="标的" />
      <el-table-column prop="priority" label="优先级" />
      <el-table-column prop="status" label="状态" :formatter="statusFormatter" />
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button size="mini" @click="toggleStatus(row)">{{ row.status===1?'停止':'启动' }}</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
<script>
import { ref, onMounted } from 'vue'
import { strategyApi } from '../api'
import { useRoute } from 'vue-router'

export default {
  setup() {
    const route = useRoute()
    const defs = ref([])
    const instances = ref([])
    const form = ref({
      strategyDefId: route.query.defId || '',
      symbol: 'AAPL',
      priority: 5,
      triggerConditions: '',
      params: ''
    })

    const loadDefs = async () => {
      const res = await strategyApi.listDef()
      defs.value = res.data
    }
    const loadInstances = async () => {
      const res = await strategyApi.listInstances()
      instances.value = res.data
    }
    const submit = async () => {
      await strategyApi.createInstance(form.value)
      await loadInstances()
    }
    const toggleStatus = async (row) => {
      const newStatus = row.status === 1 ? 0 : 1
      await strategyApi.updateStatus(row.id, newStatus)
      await loadInstances()
    }
    const statusFormatter = (row) => row.status === 1 ? '运行中' : '已停止'
    onMounted(() => { loadDefs(); loadInstances() })
    return { defs, instances, form, submit, toggleStatus, statusFormatter }
  }
}
</script>