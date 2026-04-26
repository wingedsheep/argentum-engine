package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Assert Perfection
 * {1}{G}
 * Sorcery
 *
 * Target creature you control gets +1/+0 until end of turn. It deals damage
 * equal to its power to up to one target creature an opponent controls.
 */
val AssertPerfection = card("Assert Perfection") {
    manaCost = "{1}{G}"
    typeLine = "Sorcery"
    oracleText = "Target creature you control gets +1/+0 until end of turn. It deals damage equal to its power to up to one target creature an opponent controls."

    spell {
        val myCreature = target("creature you control", Targets.CreatureYouControl)
        val theirCreature = target(
            "up to one creature an opponent controls",
            TargetCreature(optional = true, filter = TargetFilter.CreatureOpponentControls)
        )
        effect = CompositeEffect(
            listOf(
                Effects.ModifyStats(1, 0, myCreature),
                Effects.DealDamage(
                    amount = DynamicAmount.EntityProperty(EntityReference.Target(0), EntityNumericProperty.Power),
                    target = theirCreature,
                    damageSource = myCreature
                )
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "164"
        artist = "Matt Stewart"
        flavorText = "Some elves still cling to the old ways of treating outsiders."
        imageUri = "https://cards.scryfall.io/normal/front/6/9/6995b308-5582-4ca1-ab10-a536d5ca0a6d.jpg?1767732784"
        ruling("2025-11-17", "If either target is an illegal target as Assert Perfection tries to resolve, the creature you control won't deal damage.")
    }
}
