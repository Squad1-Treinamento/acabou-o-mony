/**
 * IdempotencyTest Component (Placeholder)
 * 
 * This is a temporary placeholder for Task 02.
 * Full implementation will be done in Task 04: Extract Idempotency Test Component
 */

import { DevToolsWarning } from './DevToolsWarning';

export function IdempotencyTest() {
  return (
    <div className="max-w-4xl mx-auto">
      {/* Warning Banner */}
      <DevToolsWarning />
      
      <div className="nu-card">
        <h2 className="text-xl font-bold text-nu-text-primary mb-4">
          Idempotency Test
        </h2>
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
          <p className="text-sm text-nu-text-secondary">
            ℹ️ <strong>Coming Soon</strong> - This feature will be implemented in Task 04.
          </p>
          <p className="text-sm text-nu-text-muted mt-2">
            This tool allows testing idempotency key behavior by sending duplicate payment requests.
          </p>
        </div>
        <div className="py-12 text-center">
          <p className="text-nu-text-muted">Coming soon...</p>
        </div>
      </div>
    </div>
  );
}
