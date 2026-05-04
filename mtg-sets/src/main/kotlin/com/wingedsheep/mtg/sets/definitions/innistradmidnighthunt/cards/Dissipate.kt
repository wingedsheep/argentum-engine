package com.wingedsheep.mtg.sets.definitions.innistradmidnighthunt.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Dissipate
 * {1}{U}{U}
 * Instant
 * Counter target spell. If that spell is countered this way, exile it instead
 * of putting it into its owner's graveyard.
 */
val Dissipate = card("Dissipate") {
    manaCost = "{1}{U}{U}"
    typeLine = "Instant"
    oracleText = "Counter target spell. If that spell is countered this way, exile it instead of putting it into its owner's graveyard."

    spell {
        target = Targets.Spell
        effect = Effects.CounterSpellToExile()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "49"
        artist = "David Palumbo"
        flavorText = "\"Morning light, morning light,\nChase away the fears of night.\"\n—Gavony children's rhyme"
        imageUri = "https://cards.scryfall.io/normal/front/4/6/4689b3f2-e4b7-448e-b3d4-ab33194aafb2.jpg?1634348774"
    }
}
