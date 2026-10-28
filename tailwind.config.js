/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './src/pages/**/*.{js,ts,jsx,tsx,mdx}',
    './src/components/**/*.{js,ts,jsx,tsx,mdx}',
    './src/app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        norton: {
          yellow: '#FFB800',
          yellowHover: '#E5A600',
          gold: '#FFD700',
          dark: '#111827',
          card: '#1F2937',
          slate: '#0F172A',
          border: '#374151',
          accent: '#10B981',
          danger: '#EF4444',
          warning: '#F59E0B'
        }
      }
    },
  },
  plugins: [],
}
