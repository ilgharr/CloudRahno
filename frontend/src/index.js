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
// && mvn -f ../pom.xml spring-boot:stop && mkdir -p ../src/main/resources/static && rm -rf ../src/main/resources/static/* && cp -r ./build/* ../src/main/resources/static/ && mvn -f ../pom.xml spring-boot:run
/*
* Cats are territorial
* So, when a door gets shut, they feel as if their boundaries aren't being respected,
* which causes distress. If your cat usually runs the house,
* then they will consider your entire home to be their territory.
* When adding a barrier to your cat's space, it's common to see them become distraught.
* */