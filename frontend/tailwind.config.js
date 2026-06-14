/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        brand: {
          dark: '#0B0B14',
          panel: '#131325',
          surface: '#1E1E36',
          border: '#2E2E4F',
          primary: '#6366F1',
          secondary: '#8B5CF6',
          accent: '#A855F7',
          highlight: '#22D3EE',
          muted: '#94A3B8',
          text: '#F8FAFC',
        }
      }
    },
  },
  plugins: [],
}
