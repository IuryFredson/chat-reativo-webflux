# Chat Reativo em Tempo Real

Aplicacao de chat por salas usando Spring WebFlux, WebSocket, MongoDB reativo, Redis, React, TypeScript e Docker.

## Funcionalidades

- Entrada por nome e sala
- Envio e recebimento de mensagens em tempo real via WebSocket
- Historico das ultimas 50 mensagens por sala
- Persistencia reativa em MongoDB
- Presenca online por sala com Redis e TTL
- Eventos em tempo real para entrada, saida, presenca e digitacao
- Feedback visual para erros de conexao e payloads invalidos
- Testes automatizados para mensagens, historico, presenca e normalizacao de salas
- Ambiente com Docker Compose para frontend, backend, MongoDB e Redis

## Como executar

```bash
docker compose up --build
```

Depois acesse:

```text
http://localhost:5173
```

Abra duas abas no navegador com a mesma sala para testar mensagens, presenca e digitacao em tempo real.

## Endpoints

```text
GET /api/rooms/{room}/messages
WS  /ws/chat?room={room}&author={author}
```

Mensagem enviada pelo WebSocket:

```json
{
  "type": "MESSAGE",
  "author": "Iury",
  "content": "Ola!"
}
```

Evento de digitacao enviado pelo WebSocket:

```json
{
  "type": "TYPING",
  "author": "Iury",
  "typing": true
}
```

Eventos emitidos pelo servidor:

```text
MESSAGE, PRESENCE, JOIN, LEAVE, TYPING, ERROR
```

## Testes

```bash
cd backend
mvn test
```
