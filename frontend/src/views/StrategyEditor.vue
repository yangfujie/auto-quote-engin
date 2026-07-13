<template>
  <div>
    <el-input v-model="strategyName" placeholder="策略名称" />
    <DrawflowWrapper ref="drawflow" />
    <el-button @click="save">保存策略</el-button>
  </div>
</template>
<script>
import { ref } from 'vue'
import DrawflowWrapper from '../components/DrawflowWrapper.vue'
import { strategyApi } from '../api'

export default {
  components: { DrawflowWrapper },
  setup() {
    const strategyName = ref('')
    const drawflow = ref(null)
    const save = async () => {
      try {
        const flowJson = JSON.stringify(drawflow.value.exportData())
        await strategyApi.saveDef({ name: strategyName.value, flowJson })
        alert('保存成功')
      } catch (e) {
        console.error(e)
        alert('保存失败: ' + e.message)
      }
    }
    return { strategyName, drawflow, save }
  }
}
</script>