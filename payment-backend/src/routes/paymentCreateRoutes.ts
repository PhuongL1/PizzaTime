import { Router } from "express";
import { z } from "zod";

import type { AuthenticatedRequest } from "../auth/authenticatedRequest";
import type { PaymentCreationService } from "../payments/paymentCreationService";

const createPaymentBodySchema = z
  .object({
    orderId: z.string().trim().min(1),
    requestId: z.string().trim().min(8).max(128)
  });

export function createPaymentCreateRoutes(paymentCreationService: PaymentCreationService): Router {
  const router = Router();
  router.post("/", async (request, response, next) => {
    try {
      const body = createPaymentBodySchema.parse(request.body);
      const authenticatedRequest = request as AuthenticatedRequest;
      const result = await paymentCreationService.createPayment({
        customerId: authenticatedRequest.principal.uid,
        orderId: body.orderId,
        requestId: body.requestId
      });
      response.status(200).json(result);
    } catch (error) {
      next(error);
    }
  });
  return router;
}
