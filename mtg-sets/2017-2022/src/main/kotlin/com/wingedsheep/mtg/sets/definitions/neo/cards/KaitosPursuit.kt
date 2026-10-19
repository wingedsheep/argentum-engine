package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Kaito's Pursuit — Kamigawa: Neon Dynasty #103 (canonical printing)
 * {2}{B} · Sorcery
 *
 * Target player discards two cards. Ninjas and Rogues you control gain menace until end of turn.
 *
 * The menace grant locks in the Ninjas and Rogues you control as the spell resolves; one that enters or
 * becomes a Ninja/Rogue later in the turn doesn't gain menace.
 */
val KaitosPursuit = card("Kaito's Pursuit") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target player discards two cards. Ninjas and Rogues you control gain menace until end of turn. " +
        "(They can't be blocked except by two or more creatures.)"

    spell {
        val player = target(Targets.Player)
        effect = Effects.Discard(2, player) then
            Patterns.Group.grantKeywordToAll(
                Keyword.MENACE,
                GroupFilter(
                    GameObjectFilter.Creature.withAnyOfSubtypes(listOf(Subtype.NINJA, Subtype.ROGUE)).youControl()
                ),
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "103"
        artist = "Cristi Balanescu"
        flavorText = "Kaito knew every rooftop in the city, and closed in quickly on his quarry—but he knew " +
            "nothing of the Planar Bridge until it was already too late."
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd73fa5a-be0f-49a4-a921-afefbc8fa9c5.jpg?1783923883"
    }
}
