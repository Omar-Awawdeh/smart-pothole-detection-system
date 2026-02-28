import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'

const backendPort = process.env.BACKEND_PORT || process.env.VITE_BACKEND_PORT || '3000'
const frontendPort = Number(process.env.FRONTEND_PORT || process.env.VITE_FRONTEND_PORT || '5173')

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    host: '0.0.0.0',
    port: frontendPort,
    allowedHosts: ['potholesystem.tech', 'api.potholesystem.tech'],
    proxy: {
      '/api': {
        target: process.env.API_PROXY_TARGET || `http://localhost:${backendPort}`,
        changeOrigin: true,
      },
    },
  },
})
