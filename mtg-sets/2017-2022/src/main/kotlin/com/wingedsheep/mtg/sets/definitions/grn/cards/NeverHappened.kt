package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Never Happened
 * {2}{B}
 * Sorcery
 *
 * Target opponent reveals their hand. You choose a nonland card from that player's graveyard or
 * hand and exile it.
 *
 * The choice spans both zones in one decision (the client groups mixed-zone options by zone).
 */
val NeverHappened = card("Never Happened") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose a nonland card from that player's graveyard or hand and exile it."

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

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "80"
        artist = "Aaron Miller"
        flavorText = "Dimir assassins have the precision to slay a single memory."
        imageUri = "https://cards.scryfall.io/normal/front/2/8/289d3746-9e54-4983-9daa-f1a7afbdedad.jpg?1783934172"
    }
}
