import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// Vite 配置: 开发服务器代理 /api 到后端 Spring Boot
export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
    }),
    Components({
      resolvers: [
        ElementPlusResolver(),
        // @element-plus/icons-vue 图标自动注册
        {
          type: 'global',
          resolve: (componentName) => {
            if (['Close','HomeFilled','DocumentAdd','DataAnalysis','Reading',
                 'EditPen','User','Lock','Bell','Menu','ArrowDown','SwitchButton',
                 'ArrowLeft','Edit','ChatLineSquare','Delete','TrendCharts',
                 'Collection'].includes(componentName)) {
              return { name: componentName, from: '@element-plus/icons-vue' }
            }
          },
        },
      ],
    }),
  ],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/ws': {
        target: 'http://localhost:8080/api',
        ws: true,
        changeOrigin: true
      }
    }
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('element-plus') || id.includes('@element-plus/icons-vue')) {
            return 'element-plus'
          }
          if (id.includes('node_modules/vue/') || id.includes('node_modules/pinia/')
              || id.includes('node_modules/vue-router/') || id.includes('node_modules/axios/')) {
            return 'vendor'
          }
        }
      }
    }
  }
})
