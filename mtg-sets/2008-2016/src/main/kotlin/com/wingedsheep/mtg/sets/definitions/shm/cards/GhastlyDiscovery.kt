package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Ghastly Discovery
 * {2}{U}
 * Sorcery
 *
 * Draw two cards, then discard a card.
 * Conspire (As you cast this spell, you may tap two untapped creatures you control that share a color with it. When you do, copy it.)
 *
 * - "then" is sequencing inside one resolution, so this is a single [Effects.Composite]: the draw
 *   happens first and the discard chooses from the hand the draw just produced.
 * - The discard is [Patterns.Hand].discardCards, i.e. the Gather → Select → Move pipeline with
 *   `MoveType.Discard` — not a bare move to the graveyard, so "whenever you discard" triggers and
 *   madness see it.
 * - Conspire is declared as [KeywordAbility.Conspire] only; `CardBuilder` derives
 *   `Keyword.CONSPIRE` into `keywords`, so a separate `keywords(...)` line would be redundant.
 *   This card's reminder text has no "choose a new target" clause because the spell has no targets.
 */
val GhastlyDiscovery = card("Ghastly Discovery") {
    manaCost = "{2}{U}"
    typeLine = "Sorcery"
    oracleText = "Draw two cards, then discard a card.\n" +
        "Conspire (As you cast this spell, you may tap two untapped creatures you control that share a color with it. When you do, copy it.)"

    keywordAbility(KeywordAbility.Conspire)

    spell {
        effect = Effects.Composite(
            Effects.DrawCards(2),
            Patterns.Hand.discardCards(1)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "39"
        artist = "Howard Lyon"
        flavorText = "Korrigans, spirits bound to sources of water, shriek when they come upon their own drowned corpses."
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9dbd510a-d71f-4b0c-bc6c-e403f632cbdb.jpg?1783942761"
    }
}
