import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  preview: {
    host: '0.0.0.0',
    port: 4173,
    strictPort: true,
    allowedHosts: ['.trycloudflare.com'],
    proxy: {
      '/uploads': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyRequest, request) => {
            proxyRequest.removeHeader('origin')
            proxyRequest.setHeader('X-Forwarded-Host', request.headers.host || '')
            proxyRequest.setHeader(
              'X-Forwarded-Proto',
              request.headers['x-forwarded-proto']
                || (request.headers.host?.endsWith('.trycloudflare.com') ? 'https' : 'http'),
            )
          })
        },
      },
    },
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    strictPort: true,
    allowedHosts: ['.trycloudflare.com'],
    proxy: {
      '/uploads': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyRequest, request) => {
            proxyRequest.removeHeader('origin')
            proxyRequest.setHeader('X-Forwarded-Host', request.headers.host || '')
            proxyRequest.setHeader(
              'X-Forwarded-Proto',
              request.headers['x-forwarded-proto']
                || (request.headers.host?.endsWith('.trycloudflare.com') ? 'https' : 'http'),
            )
          })
        },
      },
    },
  },
})
