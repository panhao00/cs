import { ref, onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client/dist/sockjs'
import { ElNotification } from 'element-plus'

/**
 * WebSocket composable for real-time course capacity notifications.
 *
 * Connects to /ws via SockJS + STOMP, subscribes to
 * /topic/user/{userId}/capacity, and handles OFFERING_FULL / CAPACITY_CHANGE
 * events from the backend CapacityNotificationService.
 *
 * @param {object} options
 * @param {import('vue').Ref<number>} options.userId - Current user ID (reactive)
 * @param {import('vue').Ref<string>} options.token - JWT token (reactive)
 * @returns {{ connect: Function, disconnect: Function, isConnected: import('vue').Ref<boolean>, lastEvent: import('vue').Ref<object|null> }}
 */
export function useWebSocket({ userId, token }) {
  const isConnected = ref(false)
  const lastEvent = ref(null)

  /** @type {Client|null} */
  let stompClient = null
  /** @type {import('@stomp/stompjs').StompSubscription|null} */
  let subscription = null

  const MAX_RECONNECT_DELAY = 30000
  const INITIAL_RECONNECT_DELAY = 1000
  let reconnectDelay = INITIAL_RECONNECT_DELAY
  let reconnectTimer = null

  /**
   * Handle incoming STOMP message for capacity events.
   * @param {object} message - STOMP frame parsed body
   */
  function handleCapacityMessage(message) {
    try {
      const payload = JSON.parse(message.body)
      lastEvent.value = payload

      if (payload.type === 'OFFERING_FULL') {
        ElNotification({
          title: '课程已满',
          message: payload.message || `${payload.courseId} 课程已满员 (${payload.enrolledCount}/${payload.maxCapacity})`,
          type: 'error',
          duration: 8000,
          position: 'top-right'
        })
      } else if (payload.type === 'CAPACITY_CHANGE') {
        const ratio = payload.enrolledCount / payload.maxCapacity
        if (ratio >= 0.8) {
          ElNotification({
            title: '容量预警',
            message: `${payload.courseId} 课程容量不足 (${payload.enrolledCount}/${payload.maxCapacity})`,
            type: 'warning',
            duration: 5000,
            position: 'top-right'
          })
        }
      }
    } catch {
      // Silently ignore malformed messages
    }
  }

  /**
   * Subscribe to the user-specific capacity topic.
   */
  function subscribeToCapacity() {
    if (!stompClient || !userId.value) return

    if (subscription) {
      subscription.unsubscribe()
      subscription = null
    }

    const destination = `/topic/user/${userId.value}/capacity`
    subscription = stompClient.subscribe(destination, handleCapacityMessage)
  }

  /**
   * Create and activate a new STOMP client connection.
   */
  function connect() {
    if (stompClient) {
      disconnect()
    }

    if (!userId.value || !token.value) return

    stompClient = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: {
        Authorization: `Bearer ${token.value}`
      },
      reconnectDelay: 0, // We handle reconnection ourselves
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,

      onConnect: () => {
        isConnected.value = true
        reconnectDelay = INITIAL_RECONNECT_DELAY
        subscribeToCapacity()
      },

      onStompError: (frame) => {
        isConnected.value = false
        // The framework will trigger onDisconnect after this
      },

      onDisconnect: () => {
        isConnected.value = false
        scheduleReconnect()
      },

      onWebSocketClose: () => {
        isConnected.value = false
        scheduleReconnect()
      }
    })

    stompClient.activate()
  }

  /**
   * Schedule a reconnection attempt with exponential backoff.
   */
  function scheduleReconnect() {
    if (reconnectTimer) return
    if (!userId.value || !token.value) return

    reconnectTimer = setTimeout(() => {
      reconnectTimer = null
      connect()
    }, reconnectDelay)

    reconnectDelay = Math.min(reconnectDelay * 2, MAX_RECONNECT_DELAY)
  }

  /**
   * Disconnect and clean up all resources.
   */
  function disconnect() {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }

    if (subscription) {
      subscription.unsubscribe()
      subscription = null
    }

    if (stompClient) {
      stompClient.deactivate()
      stompClient = null
    }

    isConnected.value = false
    reconnectDelay = INITIAL_RECONNECT_DELAY
  }

  // Auto-disconnect when component is unmounted
  onUnmounted(() => {
    disconnect()
  })

  return {
    connect,
    disconnect,
    isConnected,
    lastEvent
  }
}
