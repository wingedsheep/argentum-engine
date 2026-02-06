package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.sdk.targeting.TargetCreature

/**
 * Spurred Wolverine
 * {4}{R}
 * Creature — Wolverine Beast
 * 3/2
 * Tap two untapped Beasts you control: Target creature gains first strike until end of turn.
 */
val SpurredWolverine = card("Spurred Wolverine") {
    manaCost = "{4}{R}"
    typeLine = "Creature — Wolverine Beast"
    power = 3
    toughness = 2

    activatedAbility {
        cost = AbilityCost.TapPermanents(
            count = 2,
            filter = GameObjectFilter.Creature.withSubtype("Beast")
        )
        target = TargetCreature()
        effect = GrantKeywordUntilEndOfTurnEffect(Keyword.FIRST_STRIKE, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "237"
        artist = "Daren Bader"
        flavorText = "In battle, all beasts fight more fiercely when you anger the wolverines."
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4aa5493e-55a0-49ba-9027-f7547826c1d5.jpg?1562910497"
    }
}
