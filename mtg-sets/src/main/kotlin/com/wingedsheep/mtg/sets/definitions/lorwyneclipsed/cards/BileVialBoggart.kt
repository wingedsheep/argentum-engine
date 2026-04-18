package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Bile-Vial Boggart
 * {B}
 * Creature — Goblin Assassin
 * 1/1
 *
 * When this creature dies, put a -1/-1 counter on up to one target creature.
 */
val BileVialBoggart = card("Bile-Vial Boggart") {
    manaCost = "{B}"
    typeLine = "Creature — Goblin Assassin"
    power = 1
    toughness = 1
    oracleText = "When this creature dies, put a -1/-1 counter on up to one target creature."

    triggeredAbility {
        trigger = Triggers.Dies
        val creature = target("creature", TargetCreature(count = 1, optional = true))
        effect = Effects.AddCounters(Counters.MINUS_ONE_MINUS_ONE, 1, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "87"
        artist = "Slawomir Maniak"
        flavorText = "It took years to finesse his potion of necroskitter slime and bogslither teeth. " +
            "Finally, *he* would prank the fae."
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c0fcf98-1f3c-4cff-9234-7f3d0c8b22e9.jpg?1767732575"
    }
}
