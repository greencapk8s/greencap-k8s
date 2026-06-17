const SERVICE = 'greencap-demo-worker';
const INTERVAL_MS = 5000;

console.log(`[${SERVICE}] Started — concurrency=${process.env.WORKER_CONCURRENCY || 1}, queue=${process.env.WORKER_QUEUE || 'default'}`);

setInterval(() => {
    console.log(`[${SERVICE}] [${new Date().toISOString()}] Processing queue...`);
}, INTERVAL_MS);
