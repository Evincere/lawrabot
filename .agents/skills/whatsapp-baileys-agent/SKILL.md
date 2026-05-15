---
name: whatsapp-baileys-agent
description: >-
  Skill para que el agente diseñe, configure y mantenga bots y pasarelas de
  WhatsApp usando @whiskeysockets/baileys. Incluye autenticación multi-device
  (QR y pairing code), reconexión robusta, manejo de eventos, envío y recepción
  de mensajes de texto y media, descarga y subida de archivos, administración
  de grupos, estados, listas de difusión, perfiles de negocio, presencia y
  almacenamiento persistente usando la API WebSocket de WhatsApp Web.
compatibility: >-
  Node.js 18+ recomendado, entorno con acceso a Internet saliente hacia los
  endpoints de WhatsApp Web. La skill asume uso de TypeScript/JavaScript con
  el paquete @whiskeysockets/baileys desde npm.
metadata:
  library: '@whiskeysockets/baileys'
  protocol: 'WhatsApp Web multi-device API over WebSocket'
  docs: 'https://baileys.wiki'
  npm: 'https://www.npmjs.com/package/@whiskeysockets/baileys'
  github: 'https://github.com/WhiskeySockets/Baileys'
---

# Skill de agente para WhatsApp con whiskeysockets/baileys

## Objetivo de la skill

Esta skill enseña al agente a construir y mantener integraciones robustas con WhatsApp usando la librería @whiskeysockets/baileys, una API basada en WebSockets para interactuar directamente con WhatsApp Web sin Selenium ni navegadores. La skill cubre de forma autosuficiente todo el ciclo de vida: instalación, autenticación, gestión de sesiones, manejo de eventos, envío y recepción de mensajes (incluyendo media), administración de grupos/listas y buenas prácticas de seguridad y resiliencia.

El agente debe usar esta skill cuando el usuario pida crear, depurar o extender un bot de WhatsApp, una pasarela de mensajería, una integración con backend/APIs o cualquier flujo automatizado que use la librería Baileys.

## Alcance funcional

Cuando esta skill esté activa, el agente debe ser capaz de:

- Elegir e instalar la versión estable o release candidate adecuada de @whiskeysockets/baileys, usando npm o yarn, leyendo notas de release cuando sea relevante.
- Configurar el socket principal mediante `makeWASocket`, entendiendo los parámetros críticos (auth, browser, printQRInTerminal, logger, version, syncFullHistory, etc.).
- Implementar autenticación multi-device con:
  - QR en terminal.
  - Pairing code (código de emparejamiento) para números existentes.
  - Estado de autenticación persistente mediante `useMultiFileAuthState` u otras estrategias soportadas.
- Manejar eventos clave del socket via `sock.ev.on`:
  - `connection.update` para reconexión robusta y detección de logout.
  - `messages.upsert` para procesar mensajes entrantes (texto y media).
  - `creds.update` para persistir credenciales.
  - `chats.upsert`, `chats.update`, `contacts.upsert`, etc. usando el store oficial.
- Enviar mensajes de todo tipo soportado:
  - Texto simple, botones, listas, templates interactivos.
  - Imagen, audio, vídeo, documento, notas de voz, stickers (cuando la versión los soporte).
- Descargar y procesar media entrante mediante `downloadMediaMessage`, manejando streams, buffers y guardado a disco o almacenamiento externo.
- Gestionar contactos, perfiles y presencia:
  - Verificar si un JID/ID existe en WhatsApp.
  - Consultar presencia (online/typing) cuando sea posible.
  - Obtener foto de perfil y datos de perfil de negocio.
- Administrar grupos y listas de difusión:
  - Crear grupos, añadir/eliminar participantes, promover/degradar administradores.
  - Leer y cambiar descripciones, temas y configuraciones de grupo.
  - Gestionar listas de difusión y stories cuando la API lo permita.
- Trabajar con estados (status) y mensajes temporales (disappearing messages) según lo soportado por la versión usada.
- Integrar Baileys con un almacenamiento persistente usando el `store` oficial y/o bases de datos externas.
- Diseñar una estrategia de reconexión y resiliencia que respete las limitaciones de WhatsApp (Meta Coexistence, detección de automatización, bans, etc.).

## Premisas y buenas prácticas

1. **Usar siempre la documentación oficial y la wiki de Baileys como referencia primaria.**
   - La skill asume que el agente consulta `https://baileys.wiki` y el README de GitHub para confirmar firmas, opciones y cambios de versión.

2. **Preferir la versión estable más reciente, salvo que el usuario pida explícitamente una RC.**
   - Las versiones 7.x introducen mejoras como mayor fiabilidad del socket, soporte de Meta Coexistence y reducción de vectores de detección de automatización, por lo que deben considerarse cuando estén suficientemente probadas.

3. **Diseñar la integración para ser multi-device desde el inicio.**
   - Baileys implementa la API multi-device de WhatsApp y puede coexistir con clientes oficiales en el mismo número.

4. **Evitar comportamiento sospechoso para minimizar bans.**
   - Respetar límites de velocidad, timings humanos razonables y no hacer scraping masivo de contactos o grupos.
   - Utilizar Meta Coexistence cuando la versión lo soporte para alinear el comportamiento con la app oficial.

5. **Persistir el estado de forma segura.**
   - Usar `useMultiFileAuthState` o soluciones equivalentes para almacenar credenciales en disco o almacenes seguros, nunca en repositorios públicos.

6. **Diseñar el código para reconectar de forma controlada.**
   - Inspeccionar `DisconnectReason` y decidir si reconectar o limpiar el estado cuando el usuario esté deslogueado.

## Instrucciones paso a paso

### 1. Instalación y setup del proyecto

1. Crear o reutilizar un proyecto Node.js (idealmente con TypeScript) usando Node 18+.
2. Instalar Baileys desde npm:

```bash
yarn add @whiskeysockets/baileys
# o
npm install @whiskeysockets/baileys
```

3. Si el proyecto va a manejar media o almacenamiento, también instalar dependencias como `fs`, `pino` (logger), `qrcode-terminal`, y librerías HTTP/DB según necesidades.

### 2. Autenticación y creación del socket

1. Importar `makeWASocket` y `useMultiFileAuthState`:

```ts
import makeWASocket, {
  useMultiFileAuthState,
  DisconnectReason,
  Browsers,
} from '@whiskeysockets/baileys'
```

2. Crear una función asíncrona `connectToWhatsApp` que:
   - Llame a `useMultiFileAuthState('auth_info_baileys')` para obtener `state` y `saveCreds`.
   - Cree el socket con `makeWASocket({ auth: state, browser: Browsers.ubuntu('Mi Bot'), printQRInTerminal: true })`.
   - Registre `sock.ev.on('creds.update', saveCreds)` para persistir credenciales.

3. Implementar autenticación via pairing code cuando el usuario la solicite:
   - Desactivar `printQRInTerminal`.
   - Usar `sock.requestPairingCode(number)` cuando `!sock.authState.creds.registered`.

### 3. Manejo de conexión y reconexión

1. Escuchar el evento `connection.update`:

```ts
sock.ev.on('connection.update', ({ connection, lastDisconnect }) => {
  if (connection === 'close') {
    const statusCode = (lastDisconnect?.error as any)?.output?.statusCode
    const shouldReconnect = statusCode !== DisconnectReason.loggedOut
    if (shouldReconnect) {
      connectToWhatsApp()
    } else {
      // limpiar estado o notificar al usuario que debe re-autenticar
    }
  } else if (connection === 'open') {
    console.log('Conexión abierta con WhatsApp')
  }
})
```

2. El agente debe siempre decidir explícitamente qué hacer cuando el usuario queda deslogueado (por ejemplo, borrar el directorio de auth y pedir una nueva vinculación).

### 4. Manejo de mensajes entrantes

1. Registrar un handler para `messages.upsert`:

```ts
sock.ev.on('messages.upsert', async ({ messages, type }) => {
  for (const m of messages) {
    if (!m.message) continue

    const jid = m.key.remoteJid!
    // Lógica de routing: comandos, NLU, etc.
    await sock.sendMessage(jid, { text: 'Hola, recibí tu mensaje' })
  }
})
```

2. Detectar tipo de mensaje usando `getContentType` para diferenciar texto, imagen, audio, documento, etc., y actuar en consecuencia.

3. Descargar media cuando sea necesario usando `downloadMediaMessage`, preferentemente como stream para no cargar archivos grandes en memoria.

### 5. Envío de mensajes salientes

El agente debe usar siempre `sock.sendMessage(jid, content, options?)`, comprobando:

- Formato correcto del JID (número@whatsapp.net, grupo@g.us, etc.).
- Estructura del contenido según el tipo (texto, botones, listas, media).

Ejemplo simple:

```ts
await sock.sendMessage(jid, {
  text: 'Mensaje enviado con @whiskeysockets/baileys',
})
```

Para media, respetar las recomendaciones oficiales:

- Usar `{ stream }`, `{ url }` o `Buffer` según el origen.
- Dejar que Baileys se encargue del cifrado y re-subida de media cuando haga falta.

### 6. Store y persistencia de chats/contacts

1. Cuando sea necesario mantener estados de chats, contactos y mensajes en memoria, crear un `store` usando el helper oficial, leerlo desde disco (si aplica) y enlazarlo al socket:

```ts
const store = makeInMemoryStore({ logger })
store.readFromFile('./baileys_store.json')
store.bind(sock.ev)

setInterval(() => {
  store.writeToFile('./baileys_store.json')
}, 10_000)
```

2. El agente puede usar `store.chats` y `store.contacts` para implementar dashboards, sincronización con DB, métricas, etc.

### 7. Gestión de grupos, estados y perfiles

El agente debe conocer las APIs expuestas por Baileys para:

- Crear y administrar grupos (añadir/quitar participantes, cambiar tema, descripción, foto, etc.).
- Consultar y actualizar disappearing messages.
- Consultar y publicar estados cuando la versión lo soporte.
- Consultar perfil de negocio, foto de perfil y presencia de usuarios.

Siempre debe revisar la wiki y el README para la firma actualizada de cada método, ya que algunas capacidades dependen de cambios recientes de la API de WhatsApp Web y de versiones específicas de Baileys.

### 8. Consideraciones de versión y breaking changes

- La skill asume que el agente consulta la página de releases de GitHub antes de usar una versión mayor (por ejemplo, saltar de 6.x a 7.x) para revisar breaking changes, cambios en autenticación, cambios en eventos y nuevas capacidades como Meta Coexistence.
- Algunos proyectos fijan versiones RC concretas (por ejemplo, 7.0.0-rc.x) por temas de estabilidad; el agente debe respetar lo que el usuario indique, pero puede recomendar actualizar cuando existan fixes relevantes de estabilidad y reconexión.

### 9. Seguridad y cumplimiento

- Nunca registrar en logs información sensible como códigos de emparejamiento completos, credenciales o contenido privado de mensajes, salvo que el usuario pida explícitamente ejemplos artificiales.
- Evitar automatizaciones masivas que violen términos de servicio de WhatsApp (spam, scraping de agenda completa, etc.).
- Informar al usuario cuando un caso de uso pueda entrar en zona gris respecto a políticas de WhatsApp.

### 10. Patrones de uso por parte del agente

Cuando lea una petición del usuario relacionada con WhatsApp o Baileys, el agente debe:

1. Identificar si se trata de:
   - Crear un nuevo bot/pasarela desde cero.
   - Integrar Baileys en un backend existente (REST/GraphQL, queues, etc.).
   - Depurar errores de conexión, reconexión o autenticación.
   - Extender capacidades (grupos, media, estados, etc.).

2. Activar esta skill y:
   - Proponer una arquitectura mínima (por ejemplo, worker de Baileys + API HTTP + DB).
   - Generar el código base con `connectToWhatsApp`, handlers de eventos y estructura de carpetas.
   - Añadir comentarios claros sobre qué partes deben personalizarse (routing de comandos, integración con NLU, lógica de negocio).

3. Utilizar siempre ejemplos que compilen y respeten la API pública actual de Baileys según la documentación.

4. Cuando la documentación sea ambigua o haya cambios recientes, recomendar revisar el repo oficial y issues relevantes antes de asumir comportamientos.

## Ejemplos de tareas que activan la skill

- "Crea un bot de WhatsApp multi-device en Node usando @whiskeysockets/baileys que responda a comandos /start y /help".
- "Configura autenticación por pairing code con Baileys para conectar un número existente sin mostrar QR".
- "Diseña una arquitectura escalable para un gateway HTTP → WhatsApp basado en Baileys y explica cómo manejar la reconexión".
- "Descarga imágenes entrantes en un bucket S3 usando Baileys y Node".
- "Actualiza mi proyecto de Baileys de 6.x a 7.x minimizando breaking changes".

La skill debe permitir al agente resolver todas estas tareas de extremo a extremo, generando código, explicando decisiones y respetando las mejores prácticas de la librería y de WhatsApp.
