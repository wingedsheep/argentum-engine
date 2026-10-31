package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Memory Leak
 * {2}{B}
 * Sorcery
 *
 * Target opponent reveals their hand. You choose a nonland card from that player's graveyard or
 * hand and exile it.
 * Cycling {1} ({1}, Discard this card: Draw a card.)
 *
 * The choice spans both zones in one decision (the client groups mixed-zone options by zone).
 */
val MemoryLeak = card("Memory Leak") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose a nonland card from that player's graveyard or hand and exile it.\n" +
        "Cycling {1} ({1}, Discard this card: Draw a card.)"

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
                prompt = "Choose a nonland card from their hand or graveyard to exile",
                alwaysPrompt = true,
                showAllCards = true
            )
            exile(toExile, owner = opponent.asPlayer)
        }
    }

    keywordAbility(KeywordAbility.cycling("{1}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "95"
        artist = "Karl Kopinski"
        flavorText = "\"By halves does youth depart. I remember halves of names, halves of years, halves of myself.\"\n—Ogaro, wandering sage"
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b45a493-a1c2-430e-a45c-abc1ba554877.jpg?1783931059"
    }
}
