import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'
import tailwindcss from "@tailwindcss/vite"
import { fileURLToPath, URL } from "node:url";

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    tailwindcss(),
    react()
  ],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  // server: {
  //   proxy: {
  //     '/api/*': 'http://localhost:8080',
  //     '/oauth2/*': 'http://localhost:8080',
  //   },
  // },
  server: {
    proxy: {
        '/api': {
            target: 'http://localhost:8080',
        },
        '/oauth2': {
            target: 'http://localhost:8080',
        }
    }
  }
})
