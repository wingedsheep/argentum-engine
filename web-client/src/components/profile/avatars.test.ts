/**
 * The avatar catalog lives in two places — the art and names here, the accepted ids on the server
 * (`profile/Avatars.kt`). A portrait the client offers but the server rejects fails on save; one the
 * server accepts but the client has no art for renders as the initial. Keep them in lockstep.
 */
import { readFileSync, readdirSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'
import { AI_AVATARS, AVATARS, resolveAvatar } from './avatars'

// Every quoted id inside `val ids = linkedSetOf( … )` in Avatars.kt.
const avatarsKt = readFileSync(
  resolve(__dirname, '../../../../game-server/src/main/kotlin/com/wingedsheep/gameserver/profile/Avatars.kt'),
  'utf8',
)
const idsBlock = avatarsKt.slice(avatarsKt.indexOf('linkedSetOf('), avatarsKt.indexOf('\n    )', avatarsKt.indexOf('linkedSetOf(')))
const serverIds = [...idsBlock.matchAll(/"([a-z0-9-]+)"/g)].map((m) => m[1])

// …and every quoted id inside `val aiIds = linkedSetOf( … )`, the AI opponents' portraits.
const aiBlockStart = avatarsKt.indexOf('linkedSetOf(', avatarsKt.indexOf('val aiIds'))
const serverAiIds = [...avatarsKt.slice(aiBlockStart, avatarsKt.indexOf('\n    )', aiBlockStart)).matchAll(/"([a-z0-9-]+)"/g)].map((m) => m[1])

const artIds = readdirSync(resolve(__dirname, '../../assets/avatars'))
  .filter((f) => f.endsWith('.webp'))
  .map((f) => f.replace(/\.webp$/, ''))

describe('avatar catalog', () => {
  it('offers exactly the portraits the server accepts', () => {
    expect(serverIds.length).toBeGreaterThan(0)
    expect(AVATARS.map((a) => a.id).sort()).toEqual([...serverIds].sort())
  })

  it('has art for every portrait and no stray art', () => {
    expect([...artIds].sort()).toEqual(AVATARS.map((a) => a.id).sort())
    for (const a of AVATARS) expect(resolveAvatar(a.id)?.kind).toBe('preset')
  })

  it('has art for every AI portrait the server seats, and never offers one in the picker', () => {
    expect(serverAiIds.length).toBeGreaterThan(0)
    expect(AI_AVATARS.map((a) => a.id).sort()).toEqual([...serverAiIds].sort())
    for (const a of AI_AVATARS) {
      expect(resolveAvatar(a.id)?.kind).toBe('preset')
      expect(AVATARS.some((p) => p.id === a.id)).toBe(false)
    }
  })

  it('falls back to no portrait for none or an unknown id', () => {
    expect(resolveAvatar(null)).toBeUndefined()
    expect(resolveAvatar('not-a-portrait')).toBeUndefined()
    expect(resolveAvatar('card:0.1,0.2,0.5:front/a/b/0123abcd-0000-0000-0000-000000000000')?.kind).toBe('card')
  })
})
