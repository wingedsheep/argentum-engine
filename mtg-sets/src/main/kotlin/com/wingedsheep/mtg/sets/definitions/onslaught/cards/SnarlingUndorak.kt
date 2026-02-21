package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Snarling Undorak
 * {2}{G}{G}
 * Creature — Beast
 * 3/3
 * {2}{G}: Target Beast creature gets +1/+1 until end of turn.
 * Morph {1}{G}{G}
 */
val SnarlingUndorak = card("Snarling Undorak") {
    manaCost = "{2}{G}{G}"
    typeLine = "Creature — Beast"
    power = 3
    toughness = 3
    oracleText = "{2}{G}: Target Beast creature gets +1/+1 until end of turn.\nMorph {1}{G}{G}"

    activatedAbility {
        cost = Costs.Mana("{2}{G}")
        val t = target("target", TargetPermanent(
            filter = TargetFilter(GameObjectFilter.Creature.withSubtype("Beast"))
        ))
        effect = ModifyStatsEffect(1, 1, t)
    }

    morph = "{1}{G}{G}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "283"
        artist = "Justin Sweet"
        flavorText = "Most creatures in the Krosan Forest feared the Mirari's power. A few fed upon it."
        imageUri = "https://cards.scryfall.io/normal/front/0/5/05788d63-6210-44f2-9ae4-e55e9507a3a9.jpg?1562896264"
    }
}
