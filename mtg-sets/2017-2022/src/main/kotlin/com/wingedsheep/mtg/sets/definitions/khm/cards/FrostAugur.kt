package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Frost Augur — Kaldheim #56
 * {U} · Snow Creature — Human Wizard · 1/2
 *
 * {S}, {T}: Look at the top card of your library. If it's a snow card, you may reveal it and put
 * it into your hand.
 *
 * Herald's Horn's look-and-take shape with a snow filter: the looked-at card is split on "snow
 * card"; a match may be revealed into hand, and anything not taken stays on top of the library
 * unrevealed.
 */
val FrostAugur = card("Frost Augur") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Snow Creature — Human Wizard"
    oracleText = "{S}, {T}: Look at the top card of your library. If it's a snow card, you may reveal it " +
        "and put it into your hand. ({S} can be paid with one mana from a snow source.)"
    power = 1
    toughness = 2

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{S}"), Costs.Tap)
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(1))
            val matching = filter(looked, GameObjectFilter.Any.snow())
            val toHand = chooseUpTo(
                1,
                from = matching,
                selectedLabel = "Reveal and put into your hand",
                remainderLabel = "Leave on top of your library"
            )
            move(toHand, CardDestination.ToZone(Zone.HAND), revealed = true, revealToSelf = false)
        }
        description = "{S}, {T}: Look at the top card of your library. If it's a snow card, you may reveal it and put it into your hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "56"
        artist = "Cristi Balanescu"
        flavorText = "From the barest crack in the ice, she teases forth a swirling, living map of the Cosmos."
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a2ae592f-caf6-445a-970b-f8101998e657.jpg?1783928265"
    }
}
