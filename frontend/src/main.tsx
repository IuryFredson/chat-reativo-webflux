import React, { FormEvent, useEffect, useMemo, useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import './styles.css';

type ChatMessage = {
  id: string;
  room: string;
  author: string;
  content: string;
  sentAt: string;
};

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';
const WS_URL = import.meta.env.VITE_WS_URL ?? 'ws://localhost:8080/ws/chat';

function App() {
  const [author, setAuthor] = useState('Iury');
  const [room, setRoom] = useState('geral');
  const [content, setContent] = useState('');
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [status, setStatus] = useState<'connecting' | 'online' | 'offline'>('connecting');
  const socketRef = useRef<WebSocket | null>(null);
  const bottomRef = useRef<HTMLDivElement | null>(null);

  const normalizedRoom = useMemo(() => room.trim().toLowerCase() || 'geral', [room]);

  useEffect(() => {
    fetch(`${API_URL}/api/rooms/${encodeURIComponent(normalizedRoom)}/messages`)
      .then((response) => response.json())
      .then((data: ChatMessage[]) => setMessages(data))
      .catch(() => setMessages([]));

    setStatus('connecting');
    const socket = new WebSocket(`${WS_URL}?room=${encodeURIComponent(normalizedRoom)}`);
    socketRef.current = socket;

    socket.onopen = () => setStatus('online');
    socket.onclose = () => setStatus('offline');
    socket.onerror = () => setStatus('offline');
    socket.onmessage = (event) => {
      const message = JSON.parse(event.data) as ChatMessage;
      setMessages((current) => [...current, message]);
    };

    return () => socket.close();
  }, [normalizedRoom]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  function sendMessage(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const socket = socketRef.current;
    const trimmedAuthor = author.trim();
    const trimmedContent = content.trim();

    if (!socket || socket.readyState !== WebSocket.OPEN || !trimmedAuthor || !trimmedContent) {
      return;
    }

    socket.send(JSON.stringify({ author: trimmedAuthor, content: trimmedContent }));
    setContent('');
  }

  return (
    <main className="app-shell">
      <section className="chat-panel">
        <header className="chat-header">
          <div>
            <h1>Chat Reativo</h1>
            <p>Sala #{normalizedRoom}</p>
          </div>
          <span className={`status status-${status}`}>{status}</span>
        </header>

        <div className="controls">
          <label>
            Nome
            <input value={author} onChange={(event) => setAuthor(event.target.value)} maxLength={40} />
          </label>
          <label>
            Sala
            <input value={room} onChange={(event) => setRoom(event.target.value)} maxLength={40} />
          </label>
        </div>

        <div className="messages" aria-live="polite">
          {messages.map((message) => (
            <article className="message" key={message.id}>
              <div className="message-meta">
                <strong>{message.author}</strong>
                <time>{new Date(message.sentAt).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}</time>
              </div>
              <p>{message.content}</p>
            </article>
          ))}
          <div ref={bottomRef} />
        </div>

        <form className="composer" onSubmit={sendMessage}>
          <input
            value={content}
            onChange={(event) => setContent(event.target.value)}
            placeholder="Escreva uma mensagem"
            maxLength={500}
          />
          <button type="submit" disabled={status !== 'online'}>Enviar</button>
        </form>
      </section>
    </main>
  );
}

createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
