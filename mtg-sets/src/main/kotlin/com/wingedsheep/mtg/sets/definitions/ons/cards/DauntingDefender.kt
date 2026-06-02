package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.RecipientFilter

/**
 * Daunting Defender
 * {4}{W}
 * Creature — Human Cleric
 * 3/3
 * If a source would deal damage to a Cleric creature you control, prevent 1 of that damage.
 */
val DauntingDefender = card("Daunting Defender") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 3
    toughness = 3
    oracleText = "If a source would deal damage to a Cleric creature you control, prevent 1 of that damage."

    replacementEffect(
        PreventDamage(
            amount = 1,
            appliesTo = EventPattern.DamageEvent(
                recipient = RecipientFilter.Matching(
                    GameObjectFilter.Creature.withSubtype(Subtype("Cleric")).youControl()
                )
            )
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "21"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/38737f38-26bd-417c-b6b4-53f26e4e8044.jpg?1562908285"
    }
}
