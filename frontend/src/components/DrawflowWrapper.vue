<template>
  <div ref="drawflowContainer" style="height:600px;border:1px solid #ccc;"></div>
</template>

<script>
import { ref, onMounted } from 'vue'
import Drawflow from 'drawflow'
import 'drawflow/dist/drawflow.min.css'

export default {
  props: {
    nodes: { type: Array, default: () => [] },
    connections: { type: Array, default: () => [] }
  },
  emits: ['update'],
  setup(props, { emit }) {
    const drawflowContainer = ref(null)
    let editor = null

    const exportData = () => {
      return editor ? editor.export() : null
    }

    onMounted(() => {
      editor = new Drawflow(drawflowContainer.value)
      editor.reroute = false
      editor.start()

      editor.on('nodeMoved', () => emit('update', editor.export()))
      editor.on('connectionCreated', () => emit('update', editor.export()))
      editor.on('nodeRemoved', () => emit('update', editor.export()))
    })

    return { drawflowContainer, exportData }
  }
}
</script>
