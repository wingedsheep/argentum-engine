package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Defang
 * {1}{W}
 * Enchantment — Aura
 * Enchant creature
 * Prevent all damage that would be dealt by enchanted creature.
 *
 * Demonic Torment's "dealt by enchanted creature" prevention, widened from combat to all damage.
 */
val Defang = card("Defang") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nPrevent all damage that would be dealt by enchanted creature."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    replacementEffect(
        PreventDamage(
            amount = null,
            appliesTo = EventPattern.DamageEvent(
                source = GameObjectFilter.Any.attachedToBySource()
            )
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "15"
        artist = "Steven Belledin"
        flavorText = "Once vampires are disarmed, they're given wooden swords and forced to assist in the training of Midnight Duelists."
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f12fee0b-c237-4188-a718-1572f72c63ba.jpg?1783940737"
    }
}
