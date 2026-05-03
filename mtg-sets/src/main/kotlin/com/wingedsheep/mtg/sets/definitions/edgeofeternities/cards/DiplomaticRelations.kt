package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityReference
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty

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

    // Main spell effect
    spell {
        val yourCreature = target("target creature you control", Targets.CreatureYouControl)
        val opponentCreature = target("target creature an opponent controls", Targets.CreatureOpponentControls)
        
        // Get the power of your creature (after the +1/+0 bonus)
        val creaturePower = DynamicAmount.Add(
            DynamicAmount.EntityProperty(EntityReference.Target(0), EntityNumericProperty.Power),
            DynamicAmount.Fixed(1)
        )
        
        effect = Effects.ModifyStats(1, 0, yourCreature)
            .then(Effects.GrantKeyword(Keyword.VIGILANCE, yourCreature))
            .then(Effects.DealDamage(creaturePower, opponentCreature, damageSource = yourCreature))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "177†"
        artist = "Néstor Ossandón Leal"
        flavorText = "\"We will not yield until the Eumidians relinquish our Kavaron Tomorrow.\"\n—Official Kav diplomatic statement"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/143e5853-9a31-4c8d-b21d-5ef120eb6952.jpg?1774265280"
    }
}
