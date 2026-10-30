package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Agonizing Remorse
 * {1}{B}
 * Sorcery
 * Target opponent reveals their hand. You choose a nonland card from it or a card from their
 * graveyard. Exile that card. You lose 1 life.
 *
 * One pool over both zones (Doomsday / Cranial Extraction's [CardSource.FromMultipleZones]
 * gather), filtered as "nonland, or currently in the graveyard" so a graveyard land is a legal
 * pick (ruling) while a land in hand isn't. The choice is mandatory when the pool is non-empty;
 * with neither, only the life loss happens.
 */
val AgonizingRemorse = card("Agonizing Remorse") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose a nonland card from it or a card " +
        "from their graveyard. Exile that card. You lose 1 life."

    spell {
        val opponent = target(Targets.Opponent)

        effect = Effects.Pipeline {
            run(Effects.RevealHand(opponent))
            val pool = gather(
                CardSource.FromMultipleZones(
                    zones = listOf(Zone.HAND, Zone.GRAVEYARD),
                    player = opponent.asPlayer,
                    filter = GameObjectFilter.Nonland or GameObjectFilter.Any.currentlyIn(Zone.GRAVEYARD),
                )
            )
            val chosen = chooseExactly(
                1,
                from = pool,
                chooser = Chooser.Controller,
                prompt = "Choose a nonland card from their hand or a card from their graveyard to exile",
                showAllCards = true
            )
            exile(chosen, opponent.asPlayer)
            run(Effects.LoseLife(1, EffectTarget.Controller))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "83"
        artist = "Wisnu Tan"
        flavorText = "The sharp memory of Daxos's death stabbed into Elspeth's soul."
        imageUri = "https://cards.scryfall.io/normal/front/0/9/09270c65-546e-4737-a6b7-402cbb87917a.jpg?1783931572"
        ruling("2020-01-24", "If you choose a card from their graveyard, it may be a land card.")
        ruling(
            "2020-01-24",
            "You must choose a card to exile if able. If there are no nonland cards in the opponent's " +
                "hand, you must exile a card from their graveyard, and vice versa."
        )
        ruling(
            "2020-01-24",
            "If there are no nonland cards in their hand and no cards in their graveyard, then you " +
                "just lose 1 life."
        )
    }
}
