import { useMemo } from 'react'
import { useGameStore } from '../../store/gameStore'
import type { EntityId, SearchLibraryDecision } from '../../types'
import { type ResponsiveSizes } from '../../hooks/useResponsive'
import { ZoneSelectionUI, type ZoneCardInfo } from './ZoneSelectionUI'

interface LibrarySearchUIProps {
  decision: SearchLibraryDecision
  responsive: ResponsiveSizes
}

/**
 * Library search UI - uses the shared ZoneSelectionUI component.
 */
export function LibrarySearchUI({ decision, responsive }: LibrarySearchUIProps) {
  const submitDecision = useGameStore((s) => s.submitDecision)

  // Convert library cards to ZoneCardInfo format
  const cards: ZoneCardInfo[] = useMemo(() => {
    return decision.options.map((cardId) => {
      const cardInfo = decision.cards[cardId]
      return {
        id: cardId,
        name: cardInfo?.name || 'Unknown Card',
        typeLine: cardInfo?.typeLine,
        manaCost: cardInfo?.manaCost,
        imageUri: cardInfo?.imageUri,
      }
    })
  }, [decision.options, decision.cards])

  const handleConfirm = (selectedCards: EntityId[]) => {
    submitDecision(selectedCards)
  }

  return (
    <ZoneSelectionUI
      title="Search Your Library"
      prompt={decision.prompt}
      cards={cards}
      minSelections={decision.minSelections}
      maxSelections={decision.maxSelections}
      responsive={responsive}
      onConfirm={handleConfirm}
      filterDescription={`Searching for: ${decision.filterDescription}`}
      showFailToFind={decision.minSelections === 0}
      confirmText="Confirm Selection"
      failToFindText="Fail to Find"
      sortByType={true}
    />
  )
}
