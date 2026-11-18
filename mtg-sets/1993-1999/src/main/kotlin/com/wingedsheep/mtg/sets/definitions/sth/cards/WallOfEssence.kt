package com.wingedsheep.mtg.sets.definitions.sth.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.DamageType

/**
 * Wall of Essence
 * {1}{W}
 * Creature — Wall
 * 0/4
 * Defender (This creature can't attack.)
 * Whenever this creature is dealt combat damage, you gain that much life.
 *
 * Simultaneous combat damage from several sources is one trigger for the total (CR 603.2c); it
 * still triggers when the damage is lethal.
 */
val WallOfEssence = card("Wall of Essence") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Wall"
    power = 0
    toughness = 4
    oracleText = "Defender (This creature can't attack.)\n" +
        "Whenever this creature is dealt combat damage, you gain that much life."

    keywords(Keyword.DEFENDER)

    triggeredAbility {
        trigger = Triggers.self.isDealtDamage(damageType = DamageType.Combat)
        effect = Effects.GainLife(DynamicAmounts.triggerDamageAmount())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "22"
        artist = "Adam Rex"
        flavorText = "The ceiling and the floor fell in love, but only the wall knew.\n—Dal saying"
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2f237426-a657-4234-9f79-0a06558eeb39.jpg"
        ruling("2014-07-18", "Wall of Essence's triggered ability will trigger even if it's dealt lethal damage. For example, if it's dealt 7 combat damage, its ability will trigger and you'll gain 7 life.")
    }
}
