package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Otherworld Atlas
 * {4}
 * Artifact — Book
 * {T}: Put a charge counter on this artifact.
 * {T}: Each player draws a card for each charge counter on this artifact.
 *
 * The draw count is read on resolution; if the Atlas has left the battlefield by then, the
 * counter count falls back to its last-known information.
 */
val OtherworldAtlas = card("Otherworld Atlas") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact — Book"
    oracleText = "{T}: Put a charge counter on this artifact.\n" +
        "{T}: Each player draws a card for each charge counter on this artifact."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddCounters(CounterType.CHARGE, 1, EffectTarget.Self)
        description = "Put a charge counter on Otherworld Atlas"
    }
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.DrawCards(
            DynamicAmounts.countersOnSelf(CounterType.CHARGE),
            EffectTarget.PlayerRef(Player.Each)
        )
        description = "Each player draws a card per charge counter"
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "219"
        artist = "Sam Wolfe Connelly"
        flavorText = "\"Any fool can open it. But it takes a genius to decipher the blank pages.\"\n—Kordel the Cryptic"
        imageUri = "https://cards.scryfall.io/normal/front/4/6/46e4aa67-4643-42ff-8172-200498686494.jpg?1783940651"
    }
}
