FROM node:18-alpine AS build

WORKDIR /app

# Copy package files first for dependency caching
COPY package.json package-lock.json* ./

RUN npm ci

# Copy source
COPY . .

# Build production bundle
RUN npx vue-tsc --noEmit && npx vite build

FROM nginx:alpine

# Copy built files
COPY --from=build /app/dist /usr/share/nginx/html

# Nginx config for SPA + API proxy
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
