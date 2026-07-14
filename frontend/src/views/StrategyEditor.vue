<template>
  <div style="display:flex;height:100%;">
    <!-- 左侧节点面板 -->
    <div style="width:180px;padding:10px;border-right:1px solid #ccc;">
      <h4>节点列表</h4>
      <div
        v-for="item in nodePalette"
        :key="item.type"
        draggable="true"
        @dragstart="onDragStart($event, item.type)"
        style="padding:8px 12px;margin-bottom:8px;background:#f0f2f5;border:1px solid #d9d9d9;border-radius:4px;cursor:grab;user-select:none;"
      >
        {{ item.label }}
      </div>
    </div>
    <!-- 右侧画布 -->
    <div style="flex:1;display:flex;flex-direction:column;">
      <div style="padding:8px;display:flex;align-items:center;gap:8px;">
        <el-input v-model="strategyName" placeholder="策略名称" style="width:200px;" />
        <el-button @click="save">{{ editId ? '更新策略' : '保存策略' }}</el-button>
      </div>
      <DrawflowWrapper ref="drawflow" @ready="onEditorReady" @drop-node="onDropNode" />
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import DrawflowWrapper from '../components/DrawflowWrapper.vue'
import { strategyApi } from '../api'

// 节点类型配置（对应后端 MarketDataNode / CalculateNode / OutputNode）
const NODE_CONFIG = {
  MarketData: {
    inputs: 0,
    outputs: 1,
    cssClass: 'node-market-data',
    html: `<div>
      <div class="node-title">行情数据</div>
      <div><label>合约代码</label><input type="text" df-symbol placeholder="如: 600000.SH" /></div>
    </div>`,
    data: { symbol: '' }
  },
  Calculate: {
    inputs: 1,
    outputs: 1,
    cssClass: 'node-calculate',
    html: `<div>
      <div class="node-title">计算</div>
      <div><label>表达式</label><input type="text" df-expression placeholder="如: price * 1.1" /></div>
    </div>`,
    data: { expression: '' }
  },
  Output: {
    inputs: 1,
    outputs: 0,
    cssClass: 'node-output',
    html: `<div>
      <div class="node-title">输出/下单</div>
      <div><label>价格引用</label><input type="text" df-priceRef placeholder="priceRef" /></div>
      <div><label>数量引用</label><input type="text" df-volumeRef placeholder="volumeRef" /></div>
      <div><label>方向引用</label><input type="text" df-sideRef placeholder="sideRef" /></div>
    </div>`,
    data: { priceRef: '', volumeRef: '', sideRef: '' }
  }
}

const NODE_PALETTE = [
  { type: 'MarketData', label: '行情数据' },
  { type: 'Calculate', label: '计算' },
  { type: 'Output', label: '输出/下单' }
]

export default {
  components: { DrawflowWrapper },
  setup() {
    const route = useRoute()
    const editId = ref(route.params.id || null)
    const strategyName = ref('')
    const drawflow = ref(null)
    const pendingFlowJson = ref(null)
    const nodePalette = NODE_PALETTE

    const onDragStart = (event, nodeType) => {
      event.dataTransfer.setData('nodeType', nodeType)
      event.dataTransfer.effectAllowed = 'copy'
    }

    const onDropNode = ({ nodeType, posX, posY }) => {
      const config = NODE_CONFIG[nodeType]
      if (!config || !drawflow.value) return
      drawflow.value.addNode(
        nodeType,
        config.inputs,
        config.outputs,
        posX,
        posY,
        config.cssClass,
        { ...config.data },
        config.html
      )
    }

    const onEditorReady = () => {
      if (pendingFlowJson.value && drawflow.value) {
        try {
          const data = typeof pendingFlowJson.value === 'string'
            ? JSON.parse(pendingFlowJson.value)
            : pendingFlowJson.value
          drawflow.value.importData(data)
        } catch (e) {
          console.error('Failed to import flowJson:', e)
        }
        pendingFlowJson.value = null
      }
    }

    const loadStrategy = async () => {
      if (!editId.value) return
      try {
        const res = await strategyApi.getDef(editId.value)
        const def = res.data
        strategyName.value = def.name || ''
        if (def.flowJson) {
          if (drawflow.value && drawflow.value.exportData()) {
            const data = typeof def.flowJson === 'string' ? JSON.parse(def.flowJson) : def.flowJson
            drawflow.value.importData(data)
          } else {
            pendingFlowJson.value = def.flowJson
          }
        }
      } catch (e) {
        console.error(e)
        alert('加载策略失败: ' + e.message)
      }
    }

    const save = async () => {
      try {
        const flowJson = JSON.stringify(drawflow.value.exportData())
        const payload = { name: strategyName.value, flowJson }
        if (editId.value) {
          payload.id = editId.value
        }
        await strategyApi.saveDef(payload)
        alert(editId.value ? '更新成功' : '保存成功')
      } catch (e) {
        console.error(e)
        alert('保存失败: ' + e.message)
      }
    }

    onMounted(loadStrategy)

    return { strategyName, drawflow, save, editId, onEditorReady, onDragStart, onDropNode, nodePalette }
  }
}
</script>

<style scoped>
.node-title {
  font-weight: bold;
  margin-bottom: 4px;
  text-align: center;
}
</style>
