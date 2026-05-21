# Chat Reativo em Tempo Real

Aplicacao de chat por salas usando Spring WebFlux, WebSocket, MongoDB reativo, React, TypeScript e Docker.

## Funcionalidades

- Entrada por nome e sala
- Envio e recebimento de mensagens em tempo real via WebSocket
- Historico das ultimas 50 mensagens por sala
- Persistencia em MongoDB
- Ambiente com Docker Compose

## Como executar

```bash
docker compose up --build
```

Depois acesse:

```text
http://localhost:5173
```

Abra duas abas no navegador com a mesma sala para testar a comunicacao em tempo real.

## Endpoints

```text
GET /api/rooms/{room}/messages
WS  /ws/chat?room={room}
```

Mensagem enviada pelo WebSocket:

```json
{
  "author": "Iury",
  "content": "Ola!"
}
```
