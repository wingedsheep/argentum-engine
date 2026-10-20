package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Enchanted Being — Legends #12
 * {1}{W}{W} · Creature — Human · 2/2
 *
 * Prevent all combat damage that would be dealt to this creature by enchanted creatures.
 *
 * The Marble Priest [PreventDamage] replacement (combat damage only, recipient = this creature)
 * with the source narrowed to creatures that have at least one Aura attached
 * (`GameObjectFilter.Creature.enchanted()`, i.e. [com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsEnchanted]).
 */
val EnchantedBeing = card("Enchanted Being") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human"
    power = 2
    toughness = 2
    oracleText = "Prevent all combat damage that would be dealt to this creature by enchanted creatures."

    replacementEffect(
        PreventDamage(
            amount = null,
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.Self,
                source = GameObjectFilter.Creature.enchanted(),
                damageType = DamageType.Combat
            )
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "12"
        artist = "Douglas Shuler"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/94c2880d-b37a-43ea-9fee-cd5a8ed75a7e.jpg?1783948086"
    }
}
