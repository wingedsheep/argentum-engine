package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

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
    oracleText = "{2}{G}: Target Elf creature gains forestwalk until end of turn."

    activatedAbility {
        cost = Costs.Mana("{2}{G}")
        val t = target("target", TargetPermanent(
            filter = TargetFilter(GameObjectFilter.Creature.withSubtype("Elf"))
        ))
        effect = GrantKeywordUntilEndOfTurnEffect(Keyword.FORESTWALK, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "256"
        artist = "Todd Lockwood"
        flavorText = "In harsh times, the strongest currency is cooperation."
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c7d810b8-1a15-46cc-9d9d-871ac43b7036.jpg?1562942208"
    }
}
