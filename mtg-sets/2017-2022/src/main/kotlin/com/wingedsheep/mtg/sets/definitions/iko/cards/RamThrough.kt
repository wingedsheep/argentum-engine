package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ram Through
 * {1}{G}
 * Instant
 *
 * Target creature you control deals damage equal to its power to target creature you don't
 * control. If the creature you control has trample, excess damage is dealt to that creature's
 * controller instead.
 *
 * Trample is checked as the spell resolves; with it, the bite routes the excess past lethal to the
 * bitten creature's controller (deathtouch on the source makes 1 damage lethal).
 */
val RamThrough = card("Ram Through") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature you control deals damage equal to its power to target creature you " +
        "don't control. If the creature you control has trample, excess damage is dealt to that " +
        "creature's controller instead."

    spell {
        val myCreature = target(TargetFilter.CreatureYouControl)
        val theirCreature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.If(
            condition = Conditions.TargetMatchesFilter(
                GameObjectFilter.Any.withKeyword(Keyword.TRAMPLE),
                myCreature
            ),
            then = Effects.DealDamageExcessToController(
                amount = DynamicAmounts.powerOf(myCreature),
                target = theirCreature,
                damageSource = myCreature
            ),
            otherwise = Effects.DealDamage(
                amount = DynamicAmounts.powerOf(myCreature),
                target = theirCreature,
                damageSource = myCreature
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "170"
        artist = "Zoltan Boros"
        flavorText = "\"Need a medic! And some stonemasons!\"\n—Wyllon, Drannith merchant"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac0b24e7-14e7-45ee-b5d8-bdb8674b669c.jpg?1783931030"
        ruling("2020-04-17", "If either creature is an illegal target as Ram Through tries to resolve, the creature you control won't deal damage to any creature or player.")
        ruling("2020-04-17", "If the target creature you control has deathtouch, 1 damage from it is lethal.")
        ruling("2020-04-17", "Once you've determined how much damage is excess, the creature you control simultaneously deals damage to the creature and to its controller. This damage may be modified by replacement or prevention effects.")
    }
}
