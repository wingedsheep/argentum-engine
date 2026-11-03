package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Hierophant's Chalice
 * {3}
 * Artifact
 * When this artifact enters, target opponent loses 1 life and you gain 1 life.
 * {T}: Add {C}.
 */
val HierophantsChalice = card("Hierophant's Chalice") {
    manaCost = "{3}"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, target opponent loses 1 life and you gain 1 life.\n{T}: Add {C}."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(Targets.Opponent)
        effect = Effects.LoseLife(1, t) then Effects.GainLife(1)
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "240"
        artist = "John Stanko"
        flavorText = "In the Rite of Redemption, nobles take the same vows as Elenda, the first vampire, and make the same eternal sacrifice."
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6e8be52c-25ce-49da-8435-58797550f058.jpg?1783935703"
    }
}
