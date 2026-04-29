import React, { useState } from 'react'

interface DeckInputProps {
  onDeckChange: (deckList: Record<string, number>) => void
  disabled?: boolean
}

export function DeckInput({ onDeckChange, disabled = false }: DeckInputProps) {
  const [deckText, setDeckText] = useState('')

  const parseDeckText = (text: string): Record<string, number> => {
    const deckList: Record<string, number> = {}
    const lines = text.split('\n').filter(line => line.trim())

    for (const line of lines) {
      // Support formats like "4 Lightning Bolt" or "Lightning Bolt x4"
      const count = parseInt(line.match(/^(\d+)/)?.[1] || line.match(/x(\d+)$/)?.[1] || '1')
      const cardName = line.replace(/^\d+\s*/, '').replace(/\s*x\d+$/, '').trim()

      if (cardName && !isNaN(count)) {
        deckList[cardName] = count
      }
    }

    return deckList
  }

  const handleTextChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const text = e.target.value
    setDeckText(text)

    if (text.trim()) {
      const deckList = parseDeckText(text)
      onDeckChange(deckList)
    } else {
      onDeckChange({})
    }
  }

  const loadExampleDeck = (deckName: string) => {
    const exampleDecks: Record<string, string> = {
      'red_aggro': `4 Spitfire Handler
4 Skirk Commando
4 Goblin Machinist
2 Flamestick Courier
2 Act of Treason
2 Fever Charm
2 Brightstone Ritual
12 Mountain
4 Bloodfire Mentor
4 Skirk Prospector`,
      'white_weenie': `4 Daru Mender
4 Nova Cleric
4 Gustcloak Runner
2 Piety Charm
2 Defiant Strike
2 Unified Strike
16 Plains
2 Weathered Wayfarer
2 Demystify`,
      'blue_flyers': `4 Mistform Dreamer
4 Imagecrafter
4 Merfolk of the Pearl Trident
2 Complicate
2 Syncopate
2 Trickery Charm
16 Island
2 Chain of Vapor
2 Artificial Evolution`
    }

    setDeckText(exampleDecks[deckName] || '')
    if (exampleDecks[deckName]) {
      onDeckChange(parseDeckText(exampleDecks[deckName]))
    }
  }

  return (
    <div className={styles.deckInput}>
      <div className={styles.deckInputHeader}>
        <label className={styles.inputLabel}>Custom Deck (optional)</label>
        <div className={styles.exampleButtons}>
          <button
            type="button"
            onClick={() => loadExampleDeck('red_aggro')}
            className={styles.exampleButton}
            disabled={disabled}
            title="Load Red Aggro example"
          >
            Red Aggro
          </button>
          <button
            type="button"
            onClick={() => loadExampleDeck('white_weenie')}
            className={styles.exampleButton}
            disabled={disabled}
            title="Load White Weenie example"
          >
            White Weenie
          </button>
          <button
            type="button"
            onClick={() => loadExampleDeck('blue_flyers')}
            className={styles.exampleButton}
            disabled={disabled}
            title="Load Blue Flyers example"
          >
            Blue Flyers
          </button>
        </div>
      </div>

      <textarea
        value={deckText}
        onChange={handleTextChange}
        disabled={disabled}
        placeholder={`Enter your deck list (one card per line):
4 Lightning Bolt
4 Goblin Guide
12 Mountain
...or leave empty for random deck`}
        className={styles.deckTextarea}
        rows={12}
      />

      <p className={styles.deckInputHelp}>
        Format: "4 Card Name" or "Card Name x4". Leave empty for random sealed deck.
      </p>
    </div>
  )
}

const styles = {
  deckInput: 'mt-4 p-4 border border-gray-300 rounded-lg bg-gray-50',
  deckInputHeader: 'flex justify-between items-center mb-2',
  inputLabel: 'block text-sm font-medium text-gray-700 mb-2',
  exampleButtons: 'flex gap-2',
  exampleButton: 'px-3 py-1 text-xs bg-blue-500 text-white rounded hover:bg-blue-600 disabled:opacity-50 disabled:cursor-not-allowed',
  deckTextarea: 'w-full p-2 border border-gray-300 rounded font-mono text-sm resize-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500',
  deckInputHelp: 'text-xs text-gray-600 mt-2'
}
