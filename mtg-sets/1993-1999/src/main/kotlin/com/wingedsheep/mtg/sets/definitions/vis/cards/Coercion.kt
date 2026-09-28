package com.wingedsheep.mtg.sets.definitions.vis.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType

/**
 * Coercion
 * {2}{B}
 * Sorcery
 * Target opponent reveals their hand. You choose a card from it. That player discards that card.
 */
val Coercion = card("Coercion") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose a card from it. That player discards that card."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            val hand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer), revealed = true)
            val discarded = chooseExactly(1, hand, prompt = "Choose a card for that player to discard")
            move(discarded, CardDestination.ToZone(Zone.GRAVEYARD, opponent.asPlayer),
                moveType = MoveType.Discard)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "54"
        artist = "DiTerlizzi"
        flavorText = "A rhino's bargain\n—Femeref expression meaning \"a situation with no choices\""
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f3b07d33-f5f5-45cc-b2ac-360eaf2d4146.jpg?1783946995"
    }
}
