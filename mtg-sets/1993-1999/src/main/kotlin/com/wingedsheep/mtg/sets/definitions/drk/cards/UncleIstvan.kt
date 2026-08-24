package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.events.SourceFilter

/**
 * Uncle Istvan
 * {1}{B}{B}{B}
 * Creature — Human
 * 1/3
 * Prevent all damage that would be dealt to this creature by creatures.
 *
 * A continuous [PreventDamage] replacement (CR 615), the same shape as Antiquities' Argothian
 * Treefolk with a creature source filter instead of an artifact one. Deliberately *not* limited to
 * combat damage: a creature's activated or triggered ability that damages Uncle Istvan is prevented
 * too, which is what makes him unkillable in combat but perfectly vulnerable to a burn spell.
 */
val UncleIstvan = card("Uncle Istvan") {
    manaCost = "{1}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human"
    power = 1
    toughness = 3
    oracleText = "Prevent all damage that would be dealt to this creature by creatures."

    replacementEffect(
        PreventDamage(
            amount = null,
            appliesTo = EventPattern.DamageEvent(
                recipient = RecipientFilter.Self,
                source = SourceFilter.Matching(GameObjectFilter.Creature)
            )
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "54"
        artist = "Daniel Gelon"
        flavorText = "Solitude drove the old hermit insane. Now he only keeps company with " +
            "those he can catch."
        imageUri = "https://cards.scryfall.io/normal/front/8/4/848ad6d5-3a7e-4d6b-9929-36465796871f.jpg?1783947937"
    }
}
