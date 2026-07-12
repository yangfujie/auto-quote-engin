<template>
  <div style="height:600px;border:1px solid #ccc;">
    <vue-drawflow ref="drawflow" :nodes="nodes" :connections="connections" @update="onUpdate" />
  </div>
</template>
<script>
import { ref } from 'vue'
import VueDrawflow from 'vue-drawflow'

export default {
  components: { VueDrawflow },
  setup() {
    const drawflow = ref(null)
    const nodes = ref([])
    const connections = ref([])

    const exportData = () => {
      return drawflow.value.export()
    }
    const onUpdate = (data) => {
      // 处理更新
    }
    return { drawflow, nodes, connections, exportData, onUpdate }
  }
}
</script>