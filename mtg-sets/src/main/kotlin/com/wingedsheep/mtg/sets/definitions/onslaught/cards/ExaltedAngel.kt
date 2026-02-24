package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect

/**
 * Exalted Angel
 * {4}{W}{W}
 * Creature — Angel
 * 4/5
 * Flying
 * Whenever Exalted Angel deals damage, you gain that much life.
 * Morph {2}{W}{W}
 */
val ExaltedAngel = card("Exalted Angel") {
    manaCost = "{4}{W}{W}"
    typeLine = "Creature — Angel"
    power = 4
    toughness = 5
    oracleText = "Flying\nWhenever Exalted Angel deals damage, you gain that much life.\nMorph {2}{W}{W}"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.DealsDamage
        effect = GainLifeEffect(DynamicAmount.TriggerDamageAmount)
    }

    morph = "{2}{W}{W}"

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "28"
        artist = "Michael Sutfin"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2213eac-cea4-4dfd-90c4-c1f466967e2e.jpg?1562940841"
    }
}
