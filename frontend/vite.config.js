import { fileURLToPath, URL } from 'node:url'

import { defineConfig, loadEnv } from 'vite'
import fs from 'node:fs'
import path from 'node:path'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '..', '')
  let backendPort = Number(env.APP_PORT || 8081)
  try {
    const portFile = path.resolve(__dirname, '../runtime/port.json')
    if (fs.existsSync(portFile)) {
      const content = JSON.parse(fs.readFileSync(portFile, 'utf-8'))
      if (content && content.port) backendPort = Number(content.port)
    }
  } catch {}

  return {
    plugins: [
      vue(),
      vueDevTools(),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      },
    },
    server: {
      proxy: {
        '/user': {
          target: `http://localhost:${backendPort}`,
          changeOrigin: true,
        },
      },
    },
  }
})
