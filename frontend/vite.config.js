import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import tailwindcss from '@tailwindcss/vite'

const backendTarget = 'http://127.0.0.1:8080'
const userApiPrefixes = [
  '/user/card-types',
  '/user/card-applications',
  '/user/card-application-documents',
  '/user/card-txns',
  '/user/card-payments',
  '/user/card-bills',
  '/user/card-accounts',
  '/user/cards',
]

function backendProxy(label) {
  return {
    target: backendTarget,
    changeOrigin: true,
    configure(proxy) {
      proxy.on('proxyReq', (proxyReq, req) => {
        proxyReq.removeHeader('origin')
        console.info(`[vite proxy:${label}] ${req.method} ${req.url} -> ${backendTarget}`)
      })
      proxy.on('proxyRes', (proxyRes, req) => {
        console.info(`[vite proxy:${label}] ${req.method} ${req.url} <- ${proxyRes.statusCode}`)
      })
      proxy.on('error', (error, req) => {
        console.error(`[vite proxy:${label}] ${req.method} ${req.url} failed: ${error.message}`)
      })
    },
  }
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
    tailwindcss(),
  ],
  server: {
    host: true,
    proxy: {
      '/api': backendProxy('api'),
      ...Object.fromEntries(userApiPrefixes.map((prefix) => [prefix, backendProxy(prefix)])),
      '/uploads': backendProxy('uploads'),
      '/img': backendProxy('img'),
    },
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
})
