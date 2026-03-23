import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './style.css';

// id="root" の DOM ノードに React アプリをマウントする。
// StrictMode は開発時に副作用の二重実行などを通じてバグを早期発見するために使う。
ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
