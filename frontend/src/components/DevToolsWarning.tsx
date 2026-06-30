export function DevToolsWarning() {
  return (
    <div className="mb-6 bg-orange-50 border border-nu-warning/30 rounded-xl p-4">
      <div className="flex items-start gap-3">
        <span className="text-2xl">⚠️</span>
        <div>
          <h4 className="font-semibold text-nu-warning mb-1">
            Developer Tools
          </h4>
          <p className="text-sm text-nu-text-secondary leading-relaxed">
            This section is for testing and development purposes only. 
            In production, payments are created via API integration, not manually.
          </p>
        </div>
      </div>
    </div>
  );
}
