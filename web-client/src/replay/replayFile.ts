/**
 * Replay *files* — the compact record of a finished game (seed, decks, input stream), which the
 * server exports for download and re-simulates when one is uploaded back. Watching an uploaded file
 * stores nothing; the server just plays it and returns the same shape as a public replay.
 */
import type { PublicReplayData } from './reconstructSnapshots.ts'

/** Where a finished game's replay file downloads from. */
export function replayExportUrl(gameId: string): string {
  return `/api/public/replays/${encodeURIComponent(gameId)}/export`
}

/** Upload a replay file (plain or gzipped JSON) and get back frames to watch. Throws a readable message. */
export async function uploadReplayFile(file: File): Promise<PublicReplayData> {
  let response: Response
  try {
    response = await fetch('/api/public/replays/upload', {
      method: 'POST',
      headers: { 'Content-Type': 'application/octet-stream' },
      body: file,
    })
  } catch {
    throw new Error('Could not reach the server.')
  }
  if (!response.ok) {
    const body = await response.json().catch(() => null) as { error?: string } | null
    throw new Error(body?.error ?? `Could not load replay file (${response.status}).`)
  }
  return await response.json() as PublicReplayData
}
