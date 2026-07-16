import { describe, expect, it } from "vitest";

import { TrustedOrderAmountService } from "../../src/orders/trustedOrderAmountService";

const service = new TrustedOrderAmountService();

describe("TrustedOrderAmountService", () => {
  it("resolves a trusted integer-vnd snapshot", () => {
    const result = service.resolve({
      id: "order-1",
      customerId: "customer-a",
      status: "PENDING",
      paymentMethod: "DEMO",
      paymentStatus: "PENDING",
      finalTotal: 123000,
      total: 123000,
      pricingSnapshotVnd: {
        schemaVersion: 1,
        currency: "VND",
        itemsSubtotalVnd: 120000,
        discountVnd: 0,
        deliveryFeeVnd: 3000,
        totalVnd: 123000
      },
      raw: {}
    });

    expect(result.amountVnd).toBe(123000);
  });

  it("rejects when the snapshot is missing", () => {
    expect(() =>
      service.resolve({
        id: "order-1",
        customerId: "customer-a",
        status: "PENDING",
        paymentMethod: "DEMO",
        paymentStatus: "PENDING",
        raw: {}
      })
    ).toThrow("Trusted integer-VND pricing snapshot is not available");
  });

  it("rejects mismatched totals", () => {
    expect(() =>
      service.resolve({
        id: "order-1",
        customerId: "customer-a",
        status: "PENDING",
        paymentMethod: "DEMO",
        paymentStatus: "PENDING",
        total: 123000,
        pricingSnapshotVnd: {
          schemaVersion: 1,
          currency: "VND",
          itemsSubtotalVnd: 120000,
          discountVnd: 0,
          deliveryFeeVnd: 3000,
          totalVnd: 120001
        },
        raw: {}
      })
    ).toThrow();
  });
});
