package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Rabid Bite
 * {1}{G}
 * Sorcery
 *
 * Target creature you control deals damage equal to its power to target creature you don't control.
 */
val RabidBite = card("Rabid Bite") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Target creature you control deals damage equal to its power to target creature you don't control."

    spell {
        val myCreature = target("creature you control", Targets.CreatureYouControl)
        val theirCreature = target("creature you don't control", Targets.CreatureOpponentControls)
        effect = Effects.DealDamage(
            amount = DynamicAmount.EntityProperty(EntityReference.Target(0), EntityNumericProperty.Power),
            target = theirCreature,
            damageSource = myCreature
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "394"
        artist = "John Thacker"
        imageUri = "https://cards.scryfall.io/normal/front/5/3/53a73200-b798-4bfd-a431-8b94e17b70be.jpg?1721428112"
        inBooster = false
    }
}
