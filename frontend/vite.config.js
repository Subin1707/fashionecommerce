import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'
import fs from 'node:fs'
import path from 'node:path'

function strictAssetPaths() {
  const rejectMissingAsset = (root) => (request, response, next) => {
    const pathname = new URL(request.url, 'http://localhost').pathname
    if (!pathname.startsWith('/assets/')) return next()
    let relativePath
    try {
      relativePath = decodeURIComponent(pathname.slice('/assets/'.length))
    } catch {
      response.writeHead(404).end()
      return
    }
    const assetRoot = fs.realpathSync(path.resolve(root, 'assets'))
    const filePath = path.resolve(assetRoot, relativePath)
    let realFilePath
    try {
      realFilePath = fs.realpathSync(filePath)
    } catch {
      realFilePath = null
    }
    if (!realFilePath || !realFilePath.startsWith(assetRoot + path.sep) || !fs.statSync(realFilePath).isFile()) {
      response.setHeader('X-Content-Type-Options', 'nosniff')
      response.writeHead(404).end()
      return
    }
    const setHeader = response.setHeader.bind(response)
    response.setHeader = (name, value) => {
      const header = name.toLowerCase()
      if (header === 'x-content-type-options') value = 'nosniff'
      if (header === 'cache-control') value = 'public, max-age=86400'
      return setHeader(name, value)
    }
    setHeader('X-Content-Type-Options', 'nosniff')
    setHeader('Cache-Control', 'public, max-age=86400')
    next()
  }

  return {
    name: 'strict-public-assets',
    configureServer(server) {
      server.middlewares.use(rejectMissingAsset(server.config.publicDir))
    },
    configurePreviewServer(server) {
      server.middlewares.use(rejectMissingAsset(server.config.publicDir))
    },
  }
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), strictAssetPaths()],
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
