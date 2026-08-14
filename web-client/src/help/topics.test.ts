/**
 * The help registry is "one content source, two surfaces" — but nothing at runtime enforces that a
 * `topicId` resolves. `topicById` returns `undefined` and `HelpTip` renders nothing, so a typo or a
 * renamed topic *silently deletes the `?` button* rather than failing. That is the exact drift the
 * scattered `title=` tooltips suffered from, so it gets a test instead of a convention.
 */
import { describe, it, expect } from 'vitest'
import { HELP_TOPICS, HELP_SECTIONS, topicById, topicsInSection } from './topics'
import { SHORTCUTS } from './shortcuts'
import {
  CARDS_KINDS,
  TABLE_VALUES,
  cardsKindTopicId,
  tableTopicId,
  eventTopicId,
  type EventAxis,
} from '@/components/lobby/axes'
import { ROSTERS, rosterTopicId, shapeTopicId, SHAPE_IDS } from '@/components/lobby/modeMatrix'

const EVENTS: readonly EventAxis[] = ['SINGLE_GAME', 'ROUND_ROBIN']

describe('help topic registry', () => {
  it('has unique ids', () => {
    const seen = new Set<string>()
    const dupes = HELP_TOPICS.filter((t) => (seen.has(t.id) ? true : (seen.add(t.id), false)))
    expect(dupes.map((t) => t.id)).toEqual([])
  })

  it('puts every topic in a declared section', () => {
    const sections = new Set(HELP_SECTIONS.map((s) => s.id))
    expect(HELP_TOPICS.filter((t) => !sections.has(t.section)).map((t) => t.id)).toEqual([])
  })

  it('renders every topic on some /help section', () => {
    const rendered = new Set(HELP_SECTIONS.flatMap((s) => topicsInSection(s.id)).map((t) => t.id))
    expect(HELP_TOPICS.filter((t) => !rendered.has(t.id)).map((t) => t.id)).toEqual([])
  })

  it('resolves every `related` cross-reference', () => {
    const broken = HELP_TOPICS.flatMap((t) =>
      (t.related ?? []).filter((r) => !topicById(r)).map((r) => `${t.id} -> ${r}`),
    )
    expect(broken).toEqual([])
  })

  it('never relates a topic to itself', () => {
    expect(HELP_TOPICS.filter((t) => t.related?.includes(t.id)).map((t) => t.id)).toEqual([])
  })

  it('resolves every referenced shortcut id', () => {
    const ids = new Set(SHORTCUTS.map((s) => s.id))
    const broken = HELP_TOPICS.flatMap((t) =>
      (t.shortcuts ?? []).filter((s) => !ids.has(s)).map((s) => `${t.id} -> ${s}`),
    )
    expect(broken).toEqual([])
  })

  it('gives every topic a summary the popover can show', () => {
    expect(HELP_TOPICS.filter((t) => !t.summary.trim()).map((t) => t.id)).toEqual([])
  })
})

/**
 * The axis and matrix modules bind a control's `?` to whatever value is selected, by mapping the
 * value to a topic id. Every value in those (closed) domains must land on a topic that exists —
 * otherwise the `?` disappears for exactly the setting a confused player just clicked.
 */
describe('axis and wizard topic bindings', () => {
  const cases: [string, readonly string[]][] = [
    ['cardsKindTopicId', CARDS_KINDS.map(cardsKindTopicId)],
    ['tableTopicId', TABLE_VALUES.map(tableTopicId)],
    ['eventTopicId', EVENTS.map(eventTopicId)],
    ['rosterTopicId', ROSTERS.map(rosterTopicId)],
    ['shapeTopicId', SHAPE_IDS.map(shapeTopicId)],
  ]

  for (const [name, ids] of cases) {
    it(`${name} resolves for every value in its domain`, () => {
      expect(ids.filter((id) => !topicById(id))).toEqual([])
    })
  }
})
