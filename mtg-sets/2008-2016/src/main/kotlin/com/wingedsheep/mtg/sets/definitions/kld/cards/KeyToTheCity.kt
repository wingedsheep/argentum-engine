package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Key to the City
 * {2}
 * Artifact
 * {T}, Discard a card: Up to one target creature can't be blocked this turn.
 * Whenever this artifact becomes untapped, you may pay {2}. If you do, draw a card.
 */
val KeyToTheCity = card("Key to the City") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}, Discard a card: Up to one target creature can't be blocked this turn.\n" +
        "Whenever this artifact becomes untapped, you may pay {2}. If you do, draw a card."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.DiscardCard)
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, creature)
    }

    triggeredAbility {
        trigger = Triggers.self.becomesUntapped()
        effect = Effects.MayPay(
            cost = Effects.PayMana("{2}"),
            then = Effects.DrawCards(1),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "220"
        artist = "Sean Murray"
        flavorText = "It would be unfortunate if the key fell into the wrong hands."
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66154969-5c69-40ce-8bc9-c9bc5e280d4c.jpg"
    }
}
