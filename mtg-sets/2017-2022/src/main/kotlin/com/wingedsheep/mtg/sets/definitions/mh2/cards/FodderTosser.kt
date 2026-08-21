package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Fodder Tosser — Modern Horizons 2 #226
 * {3} · Artifact
 *
 * {T}, Discard a card: This artifact deals 2 damage to target player or planeswalker.
 *
 * A colourless repeatable reach outlet that turns dead cards into face damage. The discard is an
 * *activation cost*, so [Costs.DiscardCard] — "discard a card", any card in hand — rather than
 * [Costs.DiscardSelf], which is the cycling-style "discard this card". Being a cost also means it
 * is paid on activation, before the ability goes on the stack, which is what lets madness and
 * "whenever you discard" payoffs see it even if the ability is later countered.
 *
 * "Target player or planeswalker" is the pre-"any target" wording that deliberately excludes
 * creatures, so it is [Targets.PlayerOrPlaneswalker] and not [Targets.Any].
 */
val FodderTosser = card("Fodder Tosser") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}, Discard a card: This artifact deals 2 damage to target player or planeswalker."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.DiscardCard)
        val t = target("target player or planeswalker", Targets.PlayerOrPlaneswalker)
        effect = Effects.DealDamage(2, t)
        description = "{T}, Discard a card: This artifact deals 2 damage to target player or planeswalker."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "226"
        artist = "Gabor Szikszai"
        flavorText = "\"In event of siege, load copiously with: hot oil, cannonballs, caltrops, rubble, old swords, mess-hall leftovers, chamber pots, broken chairs, salt, cousin Furt . . .\"\n—Trebuchet instructions"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd401525-b874-4af2-99a3-c2c83e22547e.jpg?1783926805"
    }
}
