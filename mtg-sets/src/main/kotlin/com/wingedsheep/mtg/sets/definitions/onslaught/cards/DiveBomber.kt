package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Dive Bomber
 * {3}{W}
 * Creature — Bird Soldier
 * 2/2
 * Flying
 * {T}, Sacrifice Dive Bomber: It deals 2 damage to target attacking or blocking creature.
 */
val DiveBomber = card("Dive Bomber") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Bird Soldier"
    power = 2
    toughness = 2
    oracleText = "Flying\n{T}, Sacrifice Dive Bomber: It deals 2 damage to target attacking or blocking creature."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        target = TargetPermanent(filter = TargetFilter.AttackingOrBlockingCreature)
        effect = DealDamageEffect(2, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "26"
        artist = "Randy Gallegos"
        flavorText = "\"Your graves will lie beneath my final nest.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65162b24-8a3b-4b92-a831-6f23f809c76f.jpg?1562918845"
    }
}
