package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.sdk.scripting.targets.TargetCreature

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
    oracleText = "Tap two untapped Beasts you control: Target creature gains first strike until end of turn."

    activatedAbility {
        cost = AbilityCost.TapPermanents(
            count = 2,
            filter = GameObjectFilter.Creature.withSubtype("Beast")
        )
        val t = target("target", TargetCreature())
        effect = GrantKeywordUntilEndOfTurnEffect(Keyword.FIRST_STRIKE, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "237"
        artist = "Daren Bader"
        flavorText = "In battle, all beasts fight more fiercely when you anger the wolverines."
        imageUri = "https://cards.scryfall.io/large/front/4/6/46d7aaea-226b-4820-8db2-89dcdcbcc557.jpg?1562911611"
    }
}
