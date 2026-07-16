import { createServer } from "node:http";

import { createApp } from "./app";

const { app, env } = createApp();
const server = createServer(app);

server.headersTimeout = 15_000;
server.requestTimeout = 15_000;

server.listen(env.port, () => {
  console.info(`pizzatime-payment-backend listening on port ${env.port}`);
});

for (const signal of ["SIGINT", "SIGTERM"] as const) {
  process.on(signal, () => {
    server.close(() => {
      process.exit(0);
    });
  });
}
