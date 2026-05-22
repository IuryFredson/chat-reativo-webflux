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

type ChatEventType = 'MESSAGE' | 'PRESENCE' | 'JOIN' | 'LEAVE' | 'TYPING' | 'ERROR';

type ChatEvent = {
  type: ChatEventType;
  id?: string;
  room?: string;
  author?: string;
  content?: string;
  sentAt?: string;
  users?: string[];
  onlineCount?: number;
  typing?: boolean;
  message?: string;
};

type Status = 'connecting' | 'online' | 'offline';

type Notice = {
  id: string;
  text: string;
};

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';
const WS_URL = import.meta.env.VITE_WS_URL ?? 'ws://localhost:8080/ws/chat';

function App() {
  const [author, setAuthor] = useState('Iury');
  const [room, setRoom] = useState('geral');
  const [content, setContent] = useState('');
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [users, setUsers] = useState<string[]>([]);
  const [typingUsers, setTypingUsers] = useState<string[]>([]);
  const [notices, setNotices] = useState<Notice[]>([]);
  const [error, setError] = useState('');
  const [status, setStatus] = useState<Status>('connecting');
  const socketRef = useRef<WebSocket | null>(null);
  const bottomRef = useRef<HTMLDivElement | null>(null);
  const typingTimeoutRef = useRef<number | null>(null);

  const normalizedRoom = useMemo(() => room.trim().toLowerCase() || 'geral', [room]);
  const normalizedAuthor = useMemo(() => author.trim() || 'anonimo', [author]);

  useEffect(() => {
    let active = true;

    fetch(`${API_URL}/api/rooms/${encodeURIComponent(normalizedRoom)}/messages`)
      .then((response) => {
        if (!response.ok) {
          throw new Error('Nao foi possivel carregar o historico');
        }
        return response.json();
      })
      .then((data: ChatMessage[]) => active && setMessages(data))
      .catch((exception: Error) => active && setError(exception.message));

    setStatus('connecting');
    setError('');
    setTypingUsers([]);

    const socket = new WebSocket(
      `${WS_URL}?room=${encodeURIComponent(normalizedRoom)}&author=${encodeURIComponent(normalizedAuthor)}`,
    );
    socketRef.current = socket;

    socket.onopen = () => setStatus('online');
    socket.onclose = () => setStatus('offline');
    socket.onerror = () => {
      setStatus('offline');
      setError('A conexao em tempo real falhou');
    };
    socket.onmessage = (event) => {
      try {
        handleSocketEvent(JSON.parse(event.data) as ChatEvent);
      } catch {
        setError('O servidor enviou um evento invalido');
      }
    };

    return () => {
      active = false;
      socket.close();
    };
  }, [normalizedRoom, normalizedAuthor]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, notices]);

  function handleSocketEvent(event: ChatEvent) {
    switch (event.type) {
      case 'MESSAGE':
        if (event.id && event.room && event.author && event.content && event.sentAt) {
          setMessages((current) => [...current, event as ChatMessage]);
        }
        setTypingUsers((current) => current.filter((user) => user !== event.author));
        break;
      case 'PRESENCE':
        setUsers(event.users ?? []);
        break;
      case 'JOIN':
        addNotice(`${event.author ?? 'Alguem'} entrou na sala`);
        break;
      case 'LEAVE':
        addNotice(`${event.author ?? 'Alguem'} saiu da sala`);
        setTypingUsers((current) => current.filter((user) => user !== event.author));
        break;
      case 'TYPING':
        updateTypingUsers(event.author, Boolean(event.typing));
        break;
      case 'ERROR':
        setError(event.message ?? 'Erro no WebSocket');
        break;
    }
  }

  function addNotice(text: string) {
    const id = crypto.randomUUID();
    setNotices((current) => [...current.slice(-4), { id, text }]);
  }

  function updateTypingUsers(user: string | undefined, typing: boolean) {
    if (!user || user === normalizedAuthor) {
      return;
    }
    setTypingUsers((current) => {
      const withoutUser = current.filter((item) => item !== user);
      return typing ? [...withoutUser, user] : withoutUser;
    });
  }

  function sendTyping(typing: boolean) {
    const socket = socketRef.current;
    if (socket?.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify({ type: 'TYPING', author: normalizedAuthor, typing }));
    }
  }

  function handleContentChange(value: string) {
    setContent(value);
    sendTyping(value.trim().length > 0);

    if (typingTimeoutRef.current) {
      window.clearTimeout(typingTimeoutRef.current);
    }
    typingTimeoutRef.current = window.setTimeout(() => sendTyping(false), 900);
  }

  function sendMessage(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const socket = socketRef.current;
    const trimmedContent = content.trim();

    if (!socket || socket.readyState !== WebSocket.OPEN || !normalizedAuthor || !trimmedContent) {
      return;
    }

    socket.send(JSON.stringify({ type: 'MESSAGE', author: normalizedAuthor, content: trimmedContent }));
    sendTyping(false);
    setContent('');
  }

  const typingLabel = typingUsers.length === 0
    ? ''
    : `${typingUsers.join(', ')} ${typingUsers.length === 1 ? 'esta' : 'estao'} digitando`;

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

        {error && (
          <button className="error-banner" type="button" onClick={() => setError('')}>
            {error}
          </button>
        )}

        <div className="chat-body">
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
            {notices.map((notice) => (
              <div className="notice" key={notice.id}>{notice.text}</div>
            ))}
            <div ref={bottomRef} />
          </div>

          <aside className="presence-panel" aria-label="Usuarios online">
            <h2>Online</h2>
            <strong>{users.length}</strong>
            <ul>
              {users.map((user) => (
                <li key={user}>{user}</li>
              ))}
            </ul>
          </aside>
        </div>

        <form className="composer" onSubmit={sendMessage}>
          <div>
            <input
              value={content}
              onChange={(event) => handleContentChange(event.target.value)}
              placeholder="Escreva uma mensagem"
              maxLength={500}
            />
            <span className="typing-label">{typingLabel}</span>
          </div>
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
