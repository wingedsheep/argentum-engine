package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DynamicAmount
import com.wingedsheep.sdk.scripting.GainLifeEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.Player

/**
 * Doubtless One
 * {3}{W}
 * Creature — Cleric Avatar
 * *|*
 * Whenever Doubtless One deals damage, you gain that much life.
 * Doubtless One's power and toughness are each equal to the number of Clerics on the battlefield.
 */
val DoubtlessOne = card("Doubtless One") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Cleric Avatar"

    dynamicStats(DynamicAmount.CountBattlefield(Player.Each, GameObjectFilter.Creature.withSubtype("Cleric")))

    triggeredAbility {
        trigger = Triggers.DealsDamage
        effect = GainLifeEffect(DynamicAmount.TriggerDamageAmount)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "27"
        artist = "Justin Sweet"
        flavorText = "It is the duty of the cleric to provide comfort to the flock and to provide the wolves a worthy opponent."
        imageUri = "https://cards.scryfall.io/large/front/0/d/0dedef8a-5527-40dc-9ad9-bcee4cf30a76.jpg?1562898196"
    }
}
