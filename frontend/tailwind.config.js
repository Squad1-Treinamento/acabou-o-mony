/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        ml: {
          yellow: '#FFE600',
          'yellow-dark': '#E5CF00',
          blue: '#3483FA',
          'blue-dark': '#1259C3',
          red: '#F23D3D',
          green: '#00A650',
          orange: '#FF7733',
          bg: '#EDEDED',
          surface: '#FFFFFF',
          'text-primary': '#333333',
          'text-secondary': '#666666',
          'text-muted': '#999999',
          border: '#E5E5E5',
        },
      },
    },
  },
  plugins: [],
}
