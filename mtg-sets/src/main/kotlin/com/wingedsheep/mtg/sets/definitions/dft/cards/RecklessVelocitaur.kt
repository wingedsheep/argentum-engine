package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/** Reckless Velocitaur — Aetherdrift #144. */
val RecklessVelocitaur = card("Reckless Velocitaur") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Minotaur Pilot"
    power = 3
    toughness = 3
    oracleText = "Whenever this creature saddles a Mount or crews a Vehicle during your main " +
        "phase, that Mount or Vehicle gets +2/+0 and gains trample until end of turn."

    triggeredAbility {
        trigger = Triggers.or(Triggers.Saddles, Triggers.Crews)
        triggerCondition = Conditions.IsYourMainPhase
        effect = Effects.Composite(
            Effects.ModifyStats(2, 0, EffectTarget.TriggeringEntity),
            Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.TriggeringEntity)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "144"
        artist = "Inkognit"
        flavorText = "Repairs are tomorrow's problems."
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8edd18be-3861-4510-ba6d-38ccba60bb5b.jpg?1783907877"
    }
}
