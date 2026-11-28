package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Ghostly Possession
 * {2}{W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has flying.
 * Prevent all combat damage that would be dealt to and dealt by enchanted creature.
 *
 * Gaseous Form's two prevention shields (combat damage *to* the enchanted creature, and combat
 * damage whose source is the permanent this Aura is attached to) plus a flying grant.
 */
val GhostlyPossession = card("Ghostly Possession") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature has flying.\n" +
        "Prevent all combat damage that would be dealt to and dealt by enchanted creature."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING)
    }

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
        collectorNumber = "18"
        artist = "Howard Lyon"
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3f048d9-ca13-485d-ad92-de4695b7dc18.jpg?1783940993"
    }
}
