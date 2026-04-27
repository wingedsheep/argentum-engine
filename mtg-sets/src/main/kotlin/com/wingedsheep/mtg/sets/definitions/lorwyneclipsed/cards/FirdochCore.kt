package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Firdoch Core
 * {3}
 * Kindred Artifact — Shapeshifter
 *
 * Changeling (This card is every creature type.)
 * {T}: Add one mana of any color.
 * {4}: This artifact becomes a 4/4 artifact creature until end of turn.
 */
val FirdochCore = card("Firdoch Core") {
    manaCost = "{3}"
    typeLine = "Kindred Artifact — Shapeshifter"
    oracleText = "Changeling (This card is every creature type.)\n" +
        "{T}: Add one mana of any color.\n" +
        "{4}: This artifact becomes a 4/4 artifact creature until end of turn."

    keywords(Keyword.CHANGELING)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
    }

    activatedAbility {
        cost = Costs.Mana("{4}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 4,
            toughness = 4,
            duration = Duration.EndOfTurn
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "255"
        artist = "Jason A. Engle"
        flavorText = "As Isilu drifted past in a swirl of shadow and moonlight, the strange, half-buried stones began to wake."
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8e45cd37-bf97-4742-978d-96f96ed653cd.jpg?1767872077"
        ruling("2025-11-17", "Firdoch Core's last ability overwrites all previous effects that set its base power and toughness to specific values. Any power- or toughness-setting effects that start to apply after the ability resolves will overwrite this effect.")
        ruling("2025-11-17", "Effects that modify a creature's power and/or toughness, such as the effect of Appeal to Eirdu, will apply to the creature no matter when they started to take effect. The same is true for any counters that change its power and/or toughness and effects that switch its power and toughness.")
    }
}
