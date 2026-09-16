'use strict';

const { domainRepository } = require('../repositories/domainRepository');

/**
 * Issues an invoice document from a PaymentSucceeded event.
 *
 * Invoicing is downstream and asynchronous: when Payments stops publishing, this service
 * stays healthy and simply issues nothing, which is why a Payments incident reaches the
 * business workflow without this service itself degrading.
 *
 * Issuing is idempotent per invoice, because the event bus delivers at least once.
 */
function issueFromPaymentSucceeded(event) {
  const existing = domainRepository.findByInvoice(event.invoiceId);
  if (existing.length > 0) return existing[0];

  process.stdout.write(
    JSON.stringify({
      level: 'info',
      event: 'invoice_document.issued',
      invoiceId: event.invoiceId,
      authorizationId: event.authorizationId,
    }) + '\n',
  );

  return domainRepository.save({
    documentNumber: domainRepository.nextDocumentNumber(),
    invoiceId: event.invoiceId,
    authorizationId: event.authorizationId,
    network: event.network,
    amountMinor: event.amountMinor,
    currency: event.currency,
    taxMinor: event.taxMinor || 0,
    netMinor: event.amountMinor - (event.taxMinor || 0),
    profileId: event.profileId || null,
    captureMode: event.captureMode || null,
    status: 'issued',
  });
}

/**
 * Voids a document after its payment is reversed, so issued revenue is not overstated.
 */
function voidDocument(documentId, reason) {
  return domainRepository.update(documentId, {
    status: 'void',
    voidedAt: new Date().toISOString(),
    voidReason: reason || 'payment_reversed',
  });
}

module.exports = { issueFromPaymentSucceeded, voidDocument };
