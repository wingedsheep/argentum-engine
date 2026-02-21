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
 * Krosan Groundshaker
 * {4}{G}{G}{G}
 * Creature — Beast
 * 6/6
 * {G}: Target Beast creature gains trample until end of turn.
 */
val KrosanGroundshaker = card("Krosan Groundshaker") {
    manaCost = "{4}{G}{G}{G}"
    typeLine = "Creature — Beast"
    power = 6
    toughness = 6
    oracleText = "{G}: Target Beast creature gains trample until end of turn."

    activatedAbility {
        cost = Costs.Mana("{G}")
        val t = target("target", TargetPermanent(
            filter = TargetFilter(GameObjectFilter.Creature.withSubtype("Beast"))
        ))
        effect = GrantKeywordUntilEndOfTurnEffect(Keyword.TRAMPLE, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "271"
        artist = "Wayne England"
        flavorText = "You know it's coming when you hear the distant thunder. You know where it's been when you see the path of broken trees."
        imageUri = "https://cards.scryfall.io/normal/front/8/2/82105090-5f71-4690-9ade-187354311ae3.jpg?1562925715"
    }
}
