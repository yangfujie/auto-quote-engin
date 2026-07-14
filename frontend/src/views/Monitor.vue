<template>
  <div>
    <h3>队列深度：{{ queueDepth }}</h3>
    <el-table :data="orders" border>
      <el-table-column prop="id" label="订单ID" />
      <el-table-column prop="instanceId" label="实例ID" />
      <el-table-column prop="side" label="方向" :formatter="sideFormatter" />
      <el-table-column prop="price" label="价格" />
      <el-table-column prop="volume" label="数量" />
      <el-table-column prop="status" label="状态" :formatter="statusFormatter" />
      <el-table-column prop="createTime" label="创建时间" />
    </el-table>
  </div>
</template>
<script>
import { ref, onMounted, onUnmounted } from 'vue'
import { monitorApi } from '../api'

export default {
  setup() {
    const queueDepth = ref(0)
    const orders = ref([])
    const timer = ref(null)

    const fetchData = async () => {
      try {
        const depth = await monitorApi.getQueueDepth()
        queueDepth.value = depth.data
        const orderRes = await monitorApi.getOrders()
        orders.value = orderRes.data
        console.log('[Monitor] orders:', orderRes.data.length, 'queueDepth:', depth.data)
      } catch (e) {
        console.error('[Monitor] fetchData error:', e)
      }
    }
    const sideFormatter = (row) => row.side === 1 ? '买' : '卖'
    const statusFormatter = (row) => {
      const map = ['排队中', '已推送', '已成交', '失败']
      return map[row.status] || '未知'
    }
    onMounted(() => {
      fetchData()
      timer.value = setInterval(fetchData, 2000)
    })
    onUnmounted(() => clearInterval(timer.value))
    return { queueDepth, orders, sideFormatter, statusFormatter }
  }
}
</script>