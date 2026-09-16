'use strict';

const express = require('express');
const domainController = require('../controllers/domainController');

const router = express.Router();

// Payments publishes PaymentSucceeded asynchronously; the event bus delivers it here.
router.post('/events/payment-succeeded', domainController.ingestPaymentSucceeded);
router.get('/documents/:documentId', domainController.getDocument);
router.post('/documents/:documentId/void', domainController.voidIssuedDocument);
router.get('/documents', domainController.listDocuments);

module.exports = router;
