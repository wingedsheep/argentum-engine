package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.targeting.TargetPermanent

/**
 * Elvish Pathcutter
 * {3}{G}
 * Creature — Elf Scout
 * 1/2
 * {2}{G}: Target Elf creature gains forestwalk until end of turn.
 */
val ElvishPathcutter = card("Elvish Pathcutter") {
    manaCost = "{3}{G}"
    typeLine = "Creature — Elf Scout"
    power = 1
    toughness = 2

    activatedAbility {
        cost = Costs.Mana("{2}{G}")
        target = TargetPermanent(
            filter = TargetFilter(GameObjectFilter.Creature.withSubtype("Elf"))
        )
        effect = GrantKeywordUntilEndOfTurnEffect(Keyword.FORESTWALK, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "256"
        artist = "Todd Lockwood"
        flavorText = "In harsh times, the strongest currency is cooperation."
        imageUri = "https://cards.scryfall.io/large/front/6/0/60aa6e4a-6ce5-42e0-89c5-71fa3b7a67f2.jpg?1562913274"
    }
}
