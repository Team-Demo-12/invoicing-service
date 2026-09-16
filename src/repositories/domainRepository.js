'use strict';

const { randomUUID } = require('crypto');

/**
 * In-memory invoice-document store. The demo service keeps no external database.
 */
const store = new Map();
let sequence = 0;

const domainRepository = {
  save(record) {
    const saved = {
      documentId: randomUUID(),
      issuedAt: new Date().toISOString(),
      ...record,
    };
    store.set(saved.documentId, saved);
    return saved;
  },

  /**
   * Finance needs a human-readable identifier on a customer-facing document, so the
   * repository owns the sequence and numbering stays monotonic across callers.
   */
  nextDocumentNumber() {
    sequence += 1;
    return `INV-${new Date().getFullYear()}-${String(sequence).padStart(6, '0')}`;
  },

  update(documentId, changes) {
    const existing = store.get(documentId);
    if (!existing) return null;
    const updated = { ...existing, ...changes };
    store.set(documentId, updated);
    return updated;
  },

  findById(documentId) {
    return store.get(documentId) || null;
  },

  findByInvoice(invoiceId) {
    return [...store.values()].filter((record) => record.invoiceId === invoiceId);
  },

  all() {
    return [...store.values()];
  },

  countByStatus() {
    return [...store.values()].reduce((counts, record) => {
      counts[record.status] = (counts[record.status] || 0) + 1;
      return counts;
    }, {});
  },

  size() {
    return store.size;
  },
};

module.exports = { domainRepository };
