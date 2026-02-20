package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.LookAtFaceDownCreatureEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Aven Soulgazer
 * {3}{W}{W}
 * Creature — Bird Cleric
 * 3/3
 * Flying
 * {2}{W}: Look at target face-down creature.
 */
val AvenSoulgazer = card("Aven Soulgazer") {
    manaCost = "{3}{W}{W}"
    typeLine = "Creature — Bird Cleric"
    power = 3
    toughness = 3
    oracleText = "Flying\n{2}{W}: Look at target face-down creature."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{2}{W}")
        target = TargetPermanent(
            filter = TargetFilter(GameObjectFilter.Creature.faceDown())
        )
        effect = LookAtFaceDownCreatureEffect(EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "8"
        artist = "John Avon"
        flavorText = "\"Every question has a proper answer. Every soul has a proper place.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/1/5189f152-f075-4090-97dd-b7686d813865.jpg?1562914107"
    }
}
