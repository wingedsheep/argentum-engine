import { describe, expect, it } from 'vitest'
import { buildJoinUrl } from './joinLink'

describe('buildJoinUrl', () => {
  it('builds a /join/:lobbyId link from the given origin', () => {
    expect(buildJoinUrl('abc-123', 'https://play.example.com')).toBe(
      'https://play.example.com/join/abc-123'
    )
  })

  it('does not emit a double slash when the origin has a trailing slash', () => {
    expect(buildJoinUrl('abc', 'https://play.example.com/')).toBe(
      'https://play.example.com/join/abc'
    )
  })

  it('url-encodes a lobby id so odd characters survive the round trip', () => {
    expect(buildJoinUrl('a b/c', 'https://x.io')).toBe('https://x.io/join/a%20b%2Fc')
  })
})
