package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

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
 * Diplomatic Relations
 * {2}{G}
 * Instant
 * Target creature you control gets +1/+0 and gains vigilance until end of turn. It deals damage equal to its power to target creature an opponent controls.
 */
val DiplomaticRelations = card("Diplomatic Relations") {
    manaCost = "{2}{G}"
    typeLine = "Instant"
    oracleText = "Target creature you control gets +1/+0 and gains vigilance until end of turn. It deals damage equal to its power to target creature an opponent controls."

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
        collectorNumber = "177†"
        artist = "Néstor Ossandón Leal"
        flavorText = "\"We will not yield until the Eumidians relinquish our Kavaron Tomorrow.\"\n—Official Kav diplomatic statement"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/143e5853-9a31-4c8d-b21d-5ef120eb6952.jpg?1774265280"
    }
}
