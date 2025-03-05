import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import { BrowserRouter } from 'react-router-dom';

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
    <BrowserRouter>
        <App />
    </BrowserRouter>
);

/*
* Cats are territorial
* So, when a door gets shut, they feel as if their boundaries aren't being respected,
* which causes distress. If your cat usually runs the house,
* then they will consider your entire home to be their territory.
* When adding a barrier to your cat's space, it's common to see them become distraught.
* */