import { Router } from "express";

export function createHealthRoutes(): Router {
  const router = Router();
  router.get("/health", (_request, response) => {
    response.json({
      status: "ok",
      service: "pizzatime-payment-backend",
      version: "0.1.0"
    });
  });
  return router;
}
