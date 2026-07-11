import { useEffect } from 'react'

const APP_TITLE = 'CanastaUY'

export function usePageTitle(pageTitle?: string) {
  useEffect(() => {
    document.title = pageTitle ? `${pageTitle} | ${APP_TITLE}` : APP_TITLE
  }, [pageTitle])
}
