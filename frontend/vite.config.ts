import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { VitePWA } from 'vite-plugin-pwa'
import path from 'path'
import fs from 'fs'

// Read version from package.json
const packageJson = JSON.parse(fs.readFileSync('./package.json', 'utf-8'))
const version = packageJson.version

// Parse changelog to get latest version changes
function parseLatestChangelog(): string[] {
  try {
    const changelogPath = './CHANGELOG.md'
    if (!fs.existsSync(changelogPath)) return []

    const content = fs.readFileSync(changelogPath, 'utf-8')
    const lines = content.split('\n')

    // Find the first version header and collect items until next version header or empty section
    const changes: string[] = []
    let foundFirstVersion = false
    let collecting = false

    for (const line of lines) {
      // Version header: ## X.X.X or ## X.X.X (YYYY-MM-DD)
      if (line.startsWith('## ')) {
        if (foundFirstVersion) break // Stop at second version header
        foundFirstVersion = true
        collecting = true
        continue
      }

      if (collecting) {
        // Change item: - something
        const match = line.match(/^\s*-\s+(.+)$/)
        if (match) {
          changes.push(match[1].trim())
        }
      }
    }

    return changes
  } catch {
    return []
  }
}

const changelog = parseLatestChangelog()

export default defineConfig({
  define: {
    __APP_VERSION__: JSON.stringify(version),
    __APP_CHANGELOG__: JSON.stringify(changelog),
  },
  plugins: [
    react(),
    tailwindcss(),
    VitePWA({
      registerType: 'prompt',
      includeAssets: ['favicon.png', 'version.json'],
      manifest: {
        name: '家肴',
        short_name: '家肴',
        description: '家庭餐饮管理应用',
        theme_color: '#ffffff',
        background_color: '#ffffff',
        display: 'standalone',
        icons: [
          { src: 'favicon.png', sizes: 'any', type: 'image/png' }
        ]
      },
      workbox: {
        // 不使用 skipWaiting，让 SW 进入 waiting 状态，触发用户更新提示
      }
    }),
    // Plugin to generate version.json at build time
    {
      name: 'generate-version-json',
      buildStart() {
        const versionJson = JSON.stringify({ version, changelog }, null, 2)
        const publicDir = path.resolve(__dirname, 'public')
        if (!fs.existsSync(publicDir)) {
          fs.mkdirSync(publicDir, { recursive: true })
        }
        fs.writeFileSync(path.join(publicDir, 'version.json'), versionJson)
      }
    }
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
      '/files': 'http://localhost:8080'
    }
  }
})
