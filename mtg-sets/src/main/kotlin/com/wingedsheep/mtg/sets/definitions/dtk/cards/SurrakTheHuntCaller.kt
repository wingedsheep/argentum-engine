package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Surrak, the Hunt Caller
 * {2}{G}{G}
 * Legendary Creature — Human Warrior
 * 5/4
 *
 * Formidable — At the beginning of combat on your turn, if creatures you control have total
 * power 8 or greater, target creature you control gains haste until end of turn.
 *
 * "Formidable" is an ability word (no rules meaning); the intervening-if is a plain total-power
 * check via [Conditions.CompareAmounts] over `sumPower()`. As an intervening-if (CR 603.4) it is
 * checked both when the trigger would fire and again on resolution, so losing power in response
 * fizzles the ability.
 */
val SurrakTheHuntCaller = card("Surrak, the Hunt Caller") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Warrior"
    oracleText = "Formidable — At the beginning of combat on your turn, if creatures you control " +
        "have total power 8 or greater, target creature you control gains haste until end of turn. " +
        "(It can attack and {T} no matter when it came under your control.)"
    power = 5
    toughness = 4

    triggeredAbility {
        trigger = Triggers.BeginCombat
        triggerCondition = Conditions.CompareAmounts(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).sumPower(),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(8)
        )
        val creature = target("target creature you control", TargetCreature(filter = TargetFilter.CreatureYouControl))
        effect = Effects.GrantKeyword(Keyword.HASTE, creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "210"
        artist = "Wesley Burt"
        flavorText = "\"The greatest honor is to feed Atarka.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b374446d-44bc-4ac5-9829-8c49f0cca173.jpg?1783938575"
    }
}
