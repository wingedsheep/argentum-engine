package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.GameEvent
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.events.SourceFilter

/**
 * Sandskin
 * {2}{W}
 * Enchantment — Aura
 * Enchant creature
 * Prevent all combat damage that would be dealt to and dealt by enchanted creature.
 */
val Sandskin = card("Sandskin") {
    manaCost = "{2}{W}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nPrevent all combat damage that would be dealt to and dealt by enchanted creature."

    auraTarget = Targets.Creature

    // Prevent combat damage TO enchanted creature
    replacementEffect(
        PreventDamage(
            amount = null,
            appliesTo = GameEvent.DamageEvent(
                recipient = RecipientFilter.EnchantedCreature,
                damageType = DamageType.Combat
            )
        )
    )

    // Prevent combat damage FROM enchanted creature
    replacementEffect(
        PreventDamage(
            amount = null,
            appliesTo = GameEvent.DamageEvent(
                source = SourceFilter.EnchantedCreature,
                damageType = DamageType.Combat
            )
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "52"
        artist = "Glen Angus"
        flavorText = "\"Those who live by the sword will die by the sword. I choose to do neither.\""
        imageUri = "https://cards.scryfall.io/large/front/8/0/80b59844-c9d4-4bc1-86e6-4cc596d9165d.jpg?1562925378"
    }
}
