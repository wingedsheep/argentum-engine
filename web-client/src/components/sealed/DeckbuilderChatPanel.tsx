import { useCallback, useEffect, useRef, useState } from 'react'
import { useGameStore, type DeckBuildingState } from '@/store/gameStore.ts'

/**
 * Chat panel that talks to the deckbuilding LLM advisor on the server.
 * Subtle slide-out panel anchored to the right side of the deck builder.
 *
 * The LLM may emit actions that:
 *  - highlight cards in the pool
 *  - clear highlights
 *  - replace the current deck (main + basic lands)
 *
 * Chat history is ephemeral — it lives in this component's state and is lost
 * on reload, by design.
 */

type ChatRole = 'user' | 'assistant' | 'system'

interface ChatMessage {
  readonly role: ChatRole
  readonly content: string
  /** Lines describing actions that were executed locally for this assistant turn. */
  readonly actionNotes?: readonly string[]
}

type ServerAction =
  | { readonly type: 'highlight_cards'; readonly cardNames: readonly string[] }
  | { readonly type: 'clear_highlights' }
  | {
      readonly type: 'set_deck'
      readonly mainDeck: ReadonlyArray<{ readonly name: string; readonly count: number }>
      readonly lands: ReadonlyArray<{ readonly name: string; readonly count: number }>
    }

interface ServerResponse {
  readonly assistantMessage: string
  readonly actions: readonly ServerAction[]
  readonly error?: string | null
}

export function DeckbuilderChatPanel({ state }: { state: DeckBuildingState }) {
  const setDeck = useGameStore((s) => s.setDeck)
  const setLlmHighlights = useGameStore((s) => s.setLlmHighlights)

  const [open, setOpen] = useState(false)
  const [messages, setMessages] = useState<ChatMessage[]>(() => [
    { role: 'assistant', content: pickWelcomeMessage() },
  ])
  const [input, setInput] = useState('')
  const [sending, setSending] = useState(false)
  const scrollRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight
    }
  }, [messages, sending])

  const applyActions = useCallback((actions: readonly ServerAction[]): string[] => {
    const notes: string[] = []
    let highlightTouched = false
    for (const action of actions) {
      switch (action.type) {
        case 'highlight_cards': {
          setLlmHighlights(action.cardNames)
          highlightTouched = true
          notes.push(
            action.cardNames.length === 0
              ? 'Cleared highlights.'
              : `Highlighted ${action.cardNames.length} card${action.cardNames.length === 1 ? '' : 's'}.`,
          )
          break
        }
        case 'clear_highlights': {
          setLlmHighlights(null)
          highlightTouched = true
          notes.push('Cleared highlights.')
          break
        }
        case 'set_deck': {
          // Flatten main deck into the (deck = string[]) shape the store uses.
          const flat: string[] = []
          let mainTotal = 0
          for (const entry of action.mainDeck) {
            for (let i = 0; i < entry.count; i++) flat.push(entry.name)
            mainTotal += entry.count
          }
          const landCounts: Record<string, number> = {}
          let landTotal = 0
          for (const entry of action.lands) {
            landCounts[entry.name] = (landCounts[entry.name] ?? 0) + entry.count
            landTotal += entry.count
          }
          setDeck(flat, landCounts)
          notes.push(`Set deck: ${mainTotal} non-land + ${landTotal} basic lands.`)
          break
        }
      }
    }
    if (!highlightTouched && actions.some((a) => a.type === 'set_deck')) {
      // Building a new deck implicitly invalidates an old highlight set.
      setLlmHighlights(null)
    }
    return notes
  }, [setDeck, setLlmHighlights])

  const handleSend = useCallback(async () => {
    const text = input.trim()
    if (!text || sending) return

    const nextMessages: ChatMessage[] = [...messages, { role: 'user', content: text }]
    setMessages(nextMessages)
    setInput('')
    setSending(true)

    try {
      const payload = buildRequest(state, nextMessages)
      const response = await fetch('/api/deckbuilding/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })
      if (!response.ok) {
        const body = await response.text().catch(() => '')
        setMessages((prev) => [
          ...prev,
          {
            role: 'assistant',
            content: `Server returned ${response.status}. ${body || 'Please try again.'}`,
          },
        ])
        return
      }
      const data = (await response.json()) as ServerResponse
      const notes = applyActions(data.actions ?? [])
      const assistantMsg: ChatMessage = notes.length > 0
        ? { role: 'assistant', content: data.assistantMessage, actionNotes: notes }
        : { role: 'assistant', content: data.assistantMessage }
      setMessages((prev) => [...prev, assistantMsg])
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          content: `Network error: ${err instanceof Error ? err.message : 'unknown'}.`,
        },
      ])
    } finally {
      setSending(false)
    }
  }, [applyActions, input, messages, sending, state])

  const onKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      void handleSend()
    }
  }

  if (!open) {
    return (
      <button
        type="button"
        onClick={() => setOpen(true)}
        title="Open deckbuilder assistant"
        style={{
          position: 'fixed',
          left: 12,
          bottom: 12,
          zIndex: 1100,
          padding: '8px 14px',
          backgroundColor: '#1f3a5f',
          color: '#cfe7ff',
          border: '1px solid #3d6fa3',
          borderRadius: 999,
          cursor: 'pointer',
          fontSize: 12,
          fontWeight: 600,
          boxShadow: '0 2px 8px rgba(0,0,0,0.4)',
          letterSpacing: 0.4,
        }}
      >
        ✨ Ask the deck advisor
      </button>
    )
  }

  return (
    <div
      style={{
        position: 'fixed',
        right: 12,
        bottom: 12,
        zIndex: 1100,
        width: 360,
        maxWidth: 'calc(100vw - 24px)',
        height: 480,
        maxHeight: 'calc(100vh - 24px)',
        display: 'flex',
        flexDirection: 'column',
        backgroundColor: '#181c22',
        border: '1px solid #2c3744',
        borderRadius: 10,
        boxShadow: '0 8px 24px rgba(0,0,0,0.5)',
        overflow: 'hidden',
      }}
    >
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '8px 12px',
          backgroundColor: '#1f2731',
          borderBottom: '1px solid #2c3744',
        }}
      >
        <span style={{ color: '#cfe7ff', fontSize: 13, fontWeight: 600, letterSpacing: 0.3 }}>
          Deck Advisor
        </span>
        <button
          type="button"
          onClick={() => setOpen(false)}
          title="Close"
          style={{
            background: 'transparent',
            border: 'none',
            color: '#9ab',
            cursor: 'pointer',
            fontSize: 16,
            lineHeight: 1,
            padding: 4,
          }}
        >
          ×
        </button>
      </div>

      <div
        ref={scrollRef}
        style={{
          flex: 1,
          overflowY: 'auto',
          padding: 10,
          display: 'flex',
          flexDirection: 'column',
          gap: 8,
        }}
      >
        {messages.map((m, i) => (
          <MessageBubble key={i} message={m} />
        ))}
        {sending && <ThinkingBubble />}
      </div>

      <div
        style={{
          display: 'flex',
          gap: 6,
          padding: 8,
          borderTop: '1px solid #2c3744',
          backgroundColor: '#161a20',
        }}
      >
        <textarea
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={onKeyDown}
          rows={2}
          placeholder="Ask anything — try “highlight all removal”…"
          disabled={sending}
          style={{
            flex: 1,
            resize: 'none',
            padding: '6px 8px',
            backgroundColor: '#11151a',
            color: '#dde6ee',
            border: '1px solid #2c3744',
            borderRadius: 6,
            fontSize: 12,
            fontFamily: 'inherit',
            outline: 'none',
          }}
        />
        <button
          type="button"
          onClick={() => void handleSend()}
          disabled={sending || input.trim().length === 0}
          style={{
            padding: '0 12px',
            backgroundColor: sending || input.trim().length === 0 ? '#2c3744' : '#3d6fa3',
            color: 'white',
            border: 'none',
            borderRadius: 6,
            cursor: sending || input.trim().length === 0 ? 'not-allowed' : 'pointer',
            fontSize: 12,
            fontWeight: 600,
          }}
        >
          Send
        </button>
      </div>
    </div>
  )
}

function MessageBubble({ message }: { message: ChatMessage }) {
  const isUser = message.role === 'user'
  return (
    <div
      style={{
        alignSelf: isUser ? 'flex-end' : 'flex-start',
        maxWidth: '85%',
        backgroundColor: isUser ? '#2a4a73' : '#222a33',
        color: '#dde6ee',
        border: `1px solid ${isUser ? '#3d6fa3' : '#2c3744'}`,
        borderRadius: 8,
        padding: '6px 9px',
        fontSize: 12,
        lineHeight: 1.45,
        whiteSpace: 'pre-wrap',
        wordBreak: 'break-word',
      }}
    >
      <div>{message.content}</div>
      {message.actionNotes && message.actionNotes.length > 0 && (
        <div
          style={{
            marginTop: 6,
            paddingTop: 6,
            borderTop: '1px dashed #3a4654',
            color: '#8fbf8f',
            fontSize: 11,
          }}
        >
          {message.actionNotes.map((note, i) => (
            <div key={i}>✓ {note}</div>
          ))}
        </div>
      )}
    </div>
  )
}

function ThinkingBubble() {
  return (
    <div
      style={{
        alignSelf: 'flex-start',
        backgroundColor: '#222a33',
        color: '#8aa',
        border: '1px solid #2c3744',
        borderRadius: 8,
        padding: '6px 9px',
        fontSize: 12,
        fontStyle: 'italic',
      }}
    >
      Thinking…
    </div>
  )
}

function buildRequest(state: DeckBuildingState, messages: readonly ChatMessage[]): unknown {
  // Aggregate the pool so each card appears once with its total + currently-available count.
  const counts: Record<string, number> = {}
  for (const card of state.cardPool) {
    counts[card.name] = (counts[card.name] ?? 0) + 1
  }
  const inDeck: Record<string, number> = {}
  for (const name of state.deck) {
    inDeck[name] = (inDeck[name] ?? 0) + 1
  }

  const seen = new Set<string>()
  const pool = state.cardPool
    .filter((card) => {
      if (seen.has(card.name)) return false
      seen.add(card.name)
      return true
    })
    .map((card) => ({
      name: card.name,
      manaCost: card.manaCost,
      typeLine: card.typeLine,
      rarity: card.rarity,
      oracleText: card.oracleText ?? null,
      power: card.power ?? null,
      toughness: card.toughness ?? null,
      isDoubleFaced: card.isDoubleFaced ?? false,
      backFaceName: card.backFaceName ?? null,
      backFaceTypeLine: card.backFaceTypeLine ?? null,
      backFaceOracleText: card.backFaceOracleText ?? null,
      totalCount: counts[card.name] ?? 1,
      availableCount: Math.max(0, (counts[card.name] ?? 1) - (inDeck[card.name] ?? 0)),
    }))

  return {
    messages: messages
      .filter((m) => m.role === 'user' || m.role === 'assistant')
      .map((m) => ({ role: m.role, content: m.content })),
    pool,
    basicLands: state.basicLands.map((c) => ({ name: c.name })),
    deck: state.deck,
    landCounts: state.landCounts,
    setCodes: state.setCodes,
  }
}

const BUILD_EXAMPLES: readonly string[] = [
  'Build me a Blue-Red Wizards deck',
  'Build a mono-red aggro list from this pool',
  'Make me a Green-White creatures deck',
  'Try a Black-Green graveyard build',
  'Put together the best two-color deck you can',
  'Add 17 lands to my current deck in the right colors',
  'Replace my deck with an aggressive Boros list',
  'Build me a control deck with lots of removal',
]

const HIGHLIGHT_EXAMPLES: readonly string[] = [
  'Highlight all the removal',
  'Show me the bomb rares in my pool',
  'Highlight every creature with flying',
  'Highlight the playable Goblins',
  'Show me cards that synergize with Wizards',
  'Highlight all the ramp and mana fixing',
  'Highlight every two-mana creature',
  'Show me the cards I should never play',
]

const QUESTION_EXAMPLES: readonly string[] = [
  'Which color pair is strongest here?',
  'What archetypes does my pool support?',
  'Is my mana curve okay?',
  'What are the best cards in my pool?',
  'How does my current deck look?',
  'What am I missing for a Goblin deck?',
  'Should I splash a third color?',
  'What\'s the best removal in my colors?',
]

function pickRandom<T>(arr: readonly T[]): T {
  return arr[Math.floor(Math.random() * arr.length)] ?? arr[0]!
}

function pickExamples(): string {
  return [
    `• "${pickRandom(BUILD_EXAMPLES)}"`,
    `• "${pickRandom(HIGHLIGHT_EXAMPLES)}"`,
    `• "${pickRandom(QUESTION_EXAMPLES)}"`,
  ].join('\n')
}

const HOW_IT_WORKS =
  "I can see your pool, deck, and the set's archetypes — and I can highlight cards or rebuild your deck (40+ cards, lands included). Try:"

const WELCOME_INTROS: readonly string[] = [
  "Hey planeswalker. Before you ask — yes, I've already looked at your pool. Some of it is great, some of it is a 2/1 for three. We'll work with what we have.",
  "Welcome to the deckbuilding table. So no, you can't sneak that fifth color past me.",
  "Ah, a fresh sealed pool — the mana hums with possibility, the rares hum a bit louder.",
  "Hail. Forty cards stand between you and a 0-3 record, and I'm here to make sure they're the right forty.",
  "Your library is finite, your splash decisions even more so — luckily that's where I come in.",
  "Salutations, summoner. Before you can swing, you must build the army — and before you build the army, we should probably count the lands.",
  "A new pool, a new puzzle. I make no promises about the rares, but I'll help you make the best of them.",
  "Step into the planar workshop. The cards are laid out, the colors are waiting.",
  "Greetings, mage. Two colors, maybe three. A handful of bombs, a pile of removal, a curve that hopefully isn't all sevens.",
  "Welcome back to the drafting hall. Your pool awaits, and so do I.",
]

function pickWelcomeMessage(): string {
  const intro = pickRandom(WELCOME_INTROS)
  return `${intro}\n\n${HOW_IT_WORKS}\n${pickExamples()}`
}
