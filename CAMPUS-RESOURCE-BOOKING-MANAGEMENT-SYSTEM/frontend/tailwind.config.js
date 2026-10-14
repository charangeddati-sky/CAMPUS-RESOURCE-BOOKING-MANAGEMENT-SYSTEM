/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      fontFamily: {
        display: ['Fraunces', 'serif'],
        sans: ['Manrope', 'system-ui', 'sans-serif'],
      },
      colors: {
        grove: {
          50: '#eef7f1',
          100: '#d5eadc',
          200: '#a8d4b7',
          300: '#6fb58a',
          400: '#3d9462',
          500: '#1f8a5b',
          600: '#146c4a',
          700: '#0f3d2e',
          800: '#0b2c22',
          900: '#071a14',
        },
        brick: {
          400: '#e07a5f',
          500: '#c14a32',
          600: '#9c3724',
        },
        gold: {
          400: '#e0b84c',
          500: '#d4a017',
        },
        cream: {
          50: '#fbf8f2',
          100: '#f4efe4',
          200: '#e8dfcc',
        },
      },
      boxShadow: {
        glass: '0 8px 32px rgba(15, 61, 46, 0.12)',
      },
    },
  },
  plugins: [],
}
