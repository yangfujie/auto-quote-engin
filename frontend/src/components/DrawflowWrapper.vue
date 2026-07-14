<template>
  <div ref="drawflowContainer" style="height:600px;border:1px solid #ccc;"></div>
</template>

<script>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import Drawflow from 'drawflow'
import 'drawflow/dist/drawflow.min.css'

export default {
  props: {
    nodes: { type: Array, default: () => [] },
    connections: { type: Array, default: () => [] }
  },
  emits: ['update', 'ready', 'drop-node'],
  setup(props, { emit }) {
    const drawflowContainer = ref(null)
    let editor = null
    let readyEmitted = false

    const getEditor = () => editor

    const exportData = () => {
      return editor ? editor.export() : null
    }

    const importData = (data) => {
      if (editor && data) {
        editor.clear()
        editor.import(data)
      }
    }

    const addNode = (name, inputs, outputs, posX, posY, cssClass, data, html) => {
      if (!editor) return null
      // drawflow addNode 签名: (name, inputs, outputs, posX, posY, class, data, html, typenode)
      // 注意：第7个参数是 data，第8个参数是 html
      return editor.addNode(name, inputs, outputs, posX, posY, cssClass, data, html)
    }

    const handleDrop = (e) => {
      e.preventDefault()
      const nodeType = e.dataTransfer.getData('nodeType')
      if (!nodeType || !editor) return
      const rect = drawflowContainer.value.getBoundingClientRect()
      const posX = e.clientX - rect.left
      const posY = e.clientY - rect.top
      emit('drop-node', { nodeType, posX, posY })
    }

    const handleDragOver = (e) => {
      e.preventDefault()
    }

    onMounted(() => {
      editor = new Drawflow(drawflowContainer.value)
      editor.reroute = false
      editor.start()

      drawflowContainer.value.addEventListener('drop', handleDrop)
      drawflowContainer.value.addEventListener('dragover', handleDragOver)

      editor.on('nodeMoved', () => emit('update', editor.export()))
      editor.on('connectionCreated', () => emit('update', editor.export()))
      editor.on('nodeRemoved', () => emit('update', editor.export()))
      editor.on('nodeDataChanged', () => emit('update', editor.export()))

      setTimeout(() => {
        if (!readyEmitted) {
          readyEmitted = true
          emit('ready', editor)
        }
      }, 100)
    })

    onBeforeUnmount(() => {
      if (editor) {
        editor.removeListener('nodeMoved')
        editor.removeListener('connectionCreated')
        editor.removeListener('nodeRemoved')
        editor.removeListener('nodeDataChanged')
      }
    })

    return { drawflowContainer, getEditor, exportData, importData, addNode }
  }
}
</script>
