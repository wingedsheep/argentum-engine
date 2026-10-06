package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Cogwork Assembler
 * {3}
 * Artifact Creature — Assembly-Worker
 * 2/3
 * {7}: Create a token that's a copy of target artifact. That token gains haste. Exile it at
 * the beginning of the next end step.
 */
val CogworkAssembler = card("Cogwork Assembler") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Assembly-Worker"
    power = 2
    toughness = 3
    oracleText = "{7}: Create a token that's a copy of target artifact. That token gains haste. " +
        "Exile it at the beginning of the next end step."

    activatedAbility {
        cost = Costs.Mana("{7}")
        val artifact = target(TargetFilter(GameObjectFilter.Artifact))
        // Haste is granted on the copy itself; the token is exiled at the next end step, so it
        // never outlives the grant (same modelling as Nahiri, the Unforgiving).
        effect = Effects.CreateTokenCopyOfTarget(
            target = artifact,
            addedKeywords = setOf(Keyword.HASTE),
            exileAtStep = Step.END
        )
        timing = TimingRule.InstantSpeed
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "145"
        artist = "Joseph Meehan"
        flavorText = "Duplication is neither thievery nor flattery. It is efficiency."
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6dddacdd-bbc4-4f9b-be1c-5f2c64be3cbc.jpg?1783936731"
        ruling("2017-02-09", "The token copies exactly what was printed on the original artifact and nothing else (unless that artifact is copying something else or is a token). It doesn't copy whether that artifact is tapped or untapped, whether it has any counters on it or Auras and Equipment attached to it, or any non-copy effects that have changed its power, toughness, types, color, or so on.")
        ruling("2017-02-09", "If the copied artifact has {X} in its mana cost, X is considered to be 0.")
        ruling("2017-02-09", "Any enters-the-battlefield abilities of the copied artifact will trigger when the token enters the battlefield.")
    }
}
