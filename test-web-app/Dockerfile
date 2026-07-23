FROM node:22.22-alpine

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY index.html ./
COPY src ./src

EXPOSE 3000

CMD ["npm", "run", "dev"]
