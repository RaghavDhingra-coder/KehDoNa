import { useEffect } from 'react'

const BASE_TITLE = 'KehDoNa — Dil mein hai, toh KehDoNa.'

export function useDocumentTitle(title?: string) {
  useEffect(() => {
    document.title = title ? `${title} · KehDoNa` : BASE_TITLE
  }, [title])
}
