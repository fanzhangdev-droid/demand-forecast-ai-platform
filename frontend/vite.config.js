import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Vite の設定ファイル。
// @vitejs/plugin-react を使うことで JSX の変換と React の Fast Refresh が有効になる。
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
  },
});
