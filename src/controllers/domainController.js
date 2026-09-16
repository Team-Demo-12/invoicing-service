'use strict';

const { issueFromPaymentSucceeded, voidDocument } = require('../services/domainService');
const { domainRepository } = require('../repositories/domainRepository');

function ingestPaymentSucceeded(req, res) {
  const event = {
    invoiceId: req.body.invoiceId,
    authorizationId: req.body.authorizationId,
    network: req.body.network,
    amountMinor: req.body.amountMinor,
    currency: req.body.currency || 'USD',
    profileId: req.body.profileId || null,
    captureMode: req.body.captureMode || null,
    taxMinor: req.body.taxMinor || 0,
  };

  const record = issueFromPaymentSucceeded(event);

  return res.status(201).json({
    documentId: record.documentId,
    documentNumber: record.documentNumber,
    invoiceId: record.invoiceId,
    status: record.status,
    requestId: req.requestId,
  });
}

function getDocument(req, res) {
  const record = domainRepository.findById(req.params.documentId);
  if (!record) {
    return res.status(404).json({ error: 'DocumentNotFound', requestId: req.requestId });
  }
  return res.json(record);
}

function voidIssuedDocument(req, res) {
  const record = voidDocument(req.params.documentId, req.body.reason);
  if (!record) {
    return res.status(404).json({ error: 'DocumentNotFound', requestId: req.requestId });
  }
  return res.json({
    documentId: record.documentId,
    status: record.status,
    voidReason: record.voidReason,
    requestId: req.requestId,
  });
}

function listDocuments(req, res) {
  const { invoiceId } = req.query;
  const documents = invoiceId
    ? domainRepository.findByInvoice(invoiceId)
    : domainRepository.all();
  return res.json({ documents });
}

module.exports = { ingestPaymentSucceeded, getDocument, voidIssuedDocument, listDocuments };
