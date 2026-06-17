const http = require('http');

const PORT = process.env.APP_PORT || 3000;
const SERVICE = 'greencap-demo-api';

const server = http.createServer((req, res) => {
    const body = JSON.stringify({
        service: SERVICE,
        status: 'ok',
        timestamp: new Date().toISOString(),
        env: process.env.NODE_ENV || 'development',
    });
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(body);
});

server.listen(PORT, () => {
    console.log(`[${SERVICE}] Listening on port ${PORT}`);
});
