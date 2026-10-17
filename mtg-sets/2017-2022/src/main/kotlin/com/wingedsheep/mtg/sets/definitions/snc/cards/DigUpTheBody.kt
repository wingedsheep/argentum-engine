package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Dig Up the Body
 * {2}{B}
 * Instant
 * Casualty 1 (As you cast this spell, you may sacrifice a creature with power 1 or greater. When you do, copy this spell.)
 * Mill two cards, then you may return a creature card from your graveyard to your hand.
 *
 * Casualty 1 (CR 702.153) is the printed [KeywordAbility.Casualty]. The body is Corpse Churn's
 * shape: mill first so the milled cards are legal picks, then a `chooseUpTo(1)` over the
 * graveyard's creature cards ("you may" = choosing zero). No target, so the choice is made on
 * resolution — the copy and the original each mill and return independently.
 */
val DigUpTheBody = card("Dig Up the Body") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Casualty 1 (As you cast this spell, you may sacrifice a creature with power 1 or greater. When you do, copy this spell.)\n" +
        "Mill two cards, then you may return a creature card from your graveyard to your hand. " +
        "(To mill a card, put the top card of your library into your graveyard.)"

    keywordAbility(KeywordAbility.casualty(1))

    spell {
        effect = Effects.Pipeline {
            run(Patterns.Library.mill(2))
            val creatures = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, Filters.Creature))
            val chosen = chooseUpTo(
                1,
                from = creatures,
                showAllCards = true,
                prompt = "You may return a creature card from your graveyard to your hand",
                selectedLabel = "Return to hand",
                remainderLabel = "Leave in graveyard"
            )
            toHand(chosen)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "76"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/902abe4d-5c19-4c96-b825-e6cd4d954a84.jpg?1783923131"
    }
}
