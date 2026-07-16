import type { Response } from "express";
import { Router } from "express";

import type { DemoPaymentPageService } from "../payments/demoPaymentPageService";

export function createDemoPaymentRoutes(demoPaymentPageService: DemoPaymentPageService): Router {
  const router = Router();

  router.get("/demo/pay/:token", async (request, response, next) => {
    try {
      const result = await demoPaymentPageService.renderPaymentPage(request.params.token);
      applyPageHeaders(response);
      response.status(result.statusCode).type("html").send(result.html);
    } catch (error) {
      next(error);
    }
  });

  router.post("/demo/pay/:token/confirm", async (request, response, next) => {
    try {
      const result = await demoPaymentPageService.confirmPayment(request.params.token);
      applyPageHeaders(response);
      response.status(result.statusCode).type("html").send(result.html);
    } catch (error) {
      next(error);
    }
  });

  router.post("/demo/pay/:token/cancel", async (request, response, next) => {
    try {
      const result = await demoPaymentPageService.cancelPayment(request.params.token);
      applyPageHeaders(response);
      response.status(result.statusCode).type("html").send(result.html);
    } catch (error) {
      next(error);
    }
  });

  return router;
}

function applyPageHeaders(response: Response): void {
  response.setHeader("cache-control", "no-store");
  response.setHeader(
    "content-security-policy",
    "default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; form-action 'self'; frame-ancestors 'none'"
  );
}
