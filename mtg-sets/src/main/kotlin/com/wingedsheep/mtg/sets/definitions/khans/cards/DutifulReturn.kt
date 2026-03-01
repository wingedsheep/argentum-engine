package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Dutiful Return
 * {3}{B}
 * Sorcery
 * Return up to two target creature cards from your graveyard to your hand.
 */
val DutifulReturn = card("Dutiful Return") {
    manaCost = "{3}{B}"
    typeLine = "Sorcery"
    oracleText = "Return up to two target creature cards from your graveyard to your hand."

    spell {
        target = TargetObject(
            count = 2,
            optional = true,
            filter = TargetFilter.CreatureInYourGraveyard
        )
        effect = ForEachTargetEffect(
            effects = listOf(MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.HAND))
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "71"
        artist = "Seb McKinnon"
        flavorText = "\"We have a word for enemies too mutilated for military service: furniture.\"\n—Taigam, Sidisi's Hand"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d5f83b3a-ce34-4179-8745-059181dd79b8.jpg?1562794193"
    }
}
