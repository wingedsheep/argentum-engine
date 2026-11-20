package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Eye Gouge
 * {B}
 * Instant
 * Target creature gets -1/-1 until end of turn. If it's a Cyclops, destroy it.
 *
 * The -1/-1 is applied first, then the Cyclops check reads the target as the spell resolves.
 */
val EyeGouge = card("Eye Gouge") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature gets -1/-1 until end of turn. If it's a Cyclops, destroy it."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(-1, -1, creature) then Effects.If(
            condition = Conditions.TargetMatchesFilter(GameObjectFilter.Creature.withSubtype(Subtype.CYCLOPS), creature),
            then = Effects.Destroy(creature),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "67"
        artist = "Tyler Jacobson"
        flavorText = "One chance. One throw. One perfect hit."
        imageUri = "https://cards.scryfall.io/normal/front/9/4/94d4eb34-0529-48a0-ac52-b57cead7b651.jpg?1783939559"
        ruling("2014-02-01", "If the target creature is a Cyclops, it will get -1/-1 before it's destroyed.")
    }
}
