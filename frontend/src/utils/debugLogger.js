export function init(app) {
  const log = (msg, err) => {
    console.error(`[frontend-debug] ${msg}`, err)
  }
  app.config.errorHandler = (err, instance, info) => {
    log(`vue-error: ${info || ''}`, err)
  }
  window.addEventListener('error', (e) => {
    log('window-error', e.error || e)
  })
  window.addEventListener('unhandledrejection', (e) => {
    log('unhandled-rejection', e.reason || e)
  })
}
