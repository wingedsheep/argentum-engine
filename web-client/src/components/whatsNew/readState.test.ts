import { describe, expect, it } from 'vitest'
import type { Announcement } from './announcements'
import { ANNOUNCEMENTS } from './announcements'
import { initialReadState, isRead, markAllRead, markRead, parseReadState } from './readState'

const entry = (id: string, date: string): Announcement => ({ id, date, kind: 'set', title: id, body: '' })

const old = entry('old', '2026-01-01')
const recent = entry('recent', '2026-09-20')
const newest = entry('newest', '2026-10-01')

describe('whats-new read state', () => {
  it('starts a first-time visitor with only the last month unread', () => {
    const state = initialReadState(new Date('2026-10-04T12:00:00Z'))
    expect(isRead(state, old)).toBe(true)
    expect(isRead(state, recent)).toBe(false)
    expect(isRead(state, newest)).toBe(false)
  })

  it('marks one entry read without touching the others', () => {
    const state = markRead({ readThrough: '2026-09-01', read: [] }, recent)
    expect(isRead(state, recent)).toBe(true)
    expect(isRead(state, newest)).toBe(false)
  })

  it('marks everything read and drops the per-entry ids', () => {
    const state = markAllRead({ readThrough: '2026-09-01', read: ['recent'] }, [newest, recent, old])
    expect(state).toEqual({ readThrough: '2026-10-01', read: [] })
    expect([newest, recent, old].every((e) => isRead(state, e))).toBe(true)
  })

  it('keeps an entry added after "mark all" unread', () => {
    const state = markAllRead({ readThrough: '2026-09-01', read: [] }, [recent])
    expect(isRead(state, newest)).toBe(false)
  })

  it('treats malformed storage as no state', () => {
    expect(parseReadState(null)).toBeNull()
    expect(parseReadState('not json')).toBeNull()
    expect(parseReadState('{"read":[]}')).toBeNull()
    expect(parseReadState('{"readThrough":"2026-09-01","read":["a",3]}')).toEqual({
      readThrough: '2026-09-01',
      read: ['a'],
    })
  })
})

describe('announcements', () => {
  it('has unique ids, valid dates, and is ordered newest first', () => {
    const ids = ANNOUNCEMENTS.map((a) => a.id)
    expect(new Set(ids).size).toBe(ids.length)
    for (const a of ANNOUNCEMENTS) expect(a.date).toMatch(/^\d{4}-\d{2}-\d{2}$/)
    const dates = ANNOUNCEMENTS.map((a) => a.date)
    expect(dates).toEqual([...dates].sort().reverse())
  })

  it('gives every set announcement a set code and a "try it if you like" pitch', () => {
    for (const a of ANNOUNCEMENTS.filter((a) => a.kind === 'set')) {
      expect(a.setCode).toBeTruthy()
      expect(a.tryIf).toBeTruthy()
    }
  })
})
