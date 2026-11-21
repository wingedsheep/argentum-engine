package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry

/**
 * Psychic Intrusion
 * {3}{U}{B}
 * Sorcery
 *
 * Target opponent reveals their hand. You choose a nonland card from that player's graveyard or
 * hand and exile it. You may cast that card for as long as it remains exiled, and you may spend
 * mana as though it were mana of any color to cast that spell.
 *
 * Same shape as Covetous Urge: Memory Leak's mixed-zone choice, then a permanent cast-only grant
 * with any-color mana spending on the exiled card.
 */
val PsychicIntrusion = card("Psychic Intrusion") {
    manaCost = "{3}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose a nonland card from that player's " +
        "graveyard or hand and exile it. You may cast that card for as long as it remains exiled, " +
        "and you may spend mana as though it were mana of any color to cast that spell."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            run(Effects.RevealHand(opponent))
            val candidates = gather(
                CardSource.FromMultipleZones(
                    zones = listOf(Zone.HAND, Zone.GRAVEYARD),
                    player = opponent.asPlayer,
                    filter = GameObjectFilter.Any
                )
            )
            val toExile = chooseExactly(
                1,
                from = candidates,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Nonland,
                prompt = "Choose a nonland card from their hand or graveyard to exile — you may cast it",
                alwaysPrompt = true,
                showAllCards = true
            )
            exile(toExile, owner = opponent.asPlayer)
            run(Effects.GrantMayPlayFromExile(
                from = toExile,
                expiry = MayPlayExpiry.Permanent,
                nonLandOnly = true,
                withAnyManaColor = true
            ))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "200"
        artist = "Jaime Jones"
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2b6e1d03-ad24-4bde-ab3c-28f94e776a4e.jpg?1783939728"
    }
}
