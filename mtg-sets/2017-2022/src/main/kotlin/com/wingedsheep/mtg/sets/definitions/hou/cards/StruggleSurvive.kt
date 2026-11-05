package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Struggle // Survive — Hour of Devastation #151
 *
 * Struggle {2}{R} — Instant: Struggle deals damage to target creature equal to the number of lands
 *   you control.
 * Survive {1}{G} — Sorcery: Aftermath (Cast this spell only from your graveyard. Then exile it.)
 *   Each player shuffles their graveyard into their library.
 */
val StruggleSurvive = card("Struggle // Survive") {
    layout = CardLayout.SPLIT
    colorIdentity = "RG"

    face("Struggle") {
        manaCost = "{2}{R}"
        typeLine = "Instant"
        oracleText = "Struggle deals damage to target creature equal to the number of lands you control."

        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.DealDamage(DynamicAmounts.landsYouControl(), creature)
        }
    }

    face("Survive") {
        manaCost = "{1}{G}"
        typeLine = "Sorcery"
        oracleText = "Aftermath (Cast this spell only from your graveyard. Then exile it.)\n" +
            "Each player shuffles their graveyard into their library."
        keywords(Keyword.AFTERMATH)

        spell {
            effect = Effects.ForEachPlayer(
                players = Player.Each,
                effects = listOf(Patterns.Library.shuffleGraveyardIntoLibrary(EffectTarget.Controller)),
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "151"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76f21f0b-aaa5-4677-8398-cef98c6fac2a.jpg?1783936006"
        ruling("2017-07-14", "If a player has no cards in their graveyard when Survive resolves, that player just shuffles their library.")
        ruling("2017-04-18", "A spell with aftermath cast from a graveyard will always be exiled afterward, whether it resolves, it's countered, or it leaves the stack in some other way.")
    }
}
