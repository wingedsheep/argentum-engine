package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Gaseous Form
 * {2}{U}
 * Enchantment — Aura
 * Enchant creature
 * Prevent all combat damage that would be dealt to and dealt by enchanted creature.
 */
val GaseousForm = card("Gaseous Form") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nPrevent all combat damage that would be dealt to and dealt by enchanted creature."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    // Prevent combat damage dealt TO the enchanted creature
    replacementEffect(
        PreventDamage(
            amount = null,
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.EnchantedCreature,
                damageType = DamageType.Combat
            )
        )
    )

    // Prevent combat damage dealt BY the enchanted creature
    replacementEffect(
        PreventDamage(
            amount = null,
            appliesTo = EventPattern.DamageEvent(
                source = GameObjectFilter.Any.attachedToBySource(),
                damageType = DamageType.Combat
            )
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "59"
        artist = "Phil Foglio"
        flavorText = "\". . . [A]nd gives to airy nothing/ A local habitation and a name.\"\n—William Shakespeare, A Midsummer-Night's Dream"
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d0266dd4-31da-480b-9a44-4e217f748f06.jpg?1783948075"
    }
}
