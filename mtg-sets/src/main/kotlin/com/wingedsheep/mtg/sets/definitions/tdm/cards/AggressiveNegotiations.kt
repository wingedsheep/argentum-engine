package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.RevealHandEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Aggressive Negotiations
 * {2}{B}
 * Sorcery
 *
 * Target opponent reveals their hand. You choose a nonland card from it and exile that card.
 * Put a +1/+1 counter on up to one target creature you control.
 *
 * Composed entirely from existing primitives: the hand-strip half mirrors Cruelclaw's Heist /
 * Tsabo's Decree (RevealHand → Gather nonland cards from the revealed hand → controller chooses
 * one → exile it). The counter half targets "up to one" creature you control via an optional
 * target; if the player declines the optional target, [Effects.AddCounters] resolves with no
 * legal target and places no counter.
 */
val AggressiveNegotiations = card("Aggressive Negotiations") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose a nonland card from it and " +
        "exile that card. Put a +1/+1 counter on up to one target creature you control."

    spell {
        val opponent = target("target opponent", Targets.Opponent)
        val creature = target(
            "creature you control",
            TargetCreature(optional = true, filter = TargetFilter.CreatureYouControl)
        )

        effect = Effects.Composite(
            listOf(
                RevealHandEffect(opponent),
                GatherCardsEffect(
                    source = CardSource.FromZone(
                        zone = Zone.HAND,
                        player = Player.ContextPlayer(0),
                        filter = GameObjectFilter.Nonland,
                    ),
                    storeAs = "revealedNonland",
                ),
                SelectFromCollectionEffect(
                    from = "revealedNonland",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    chooser = Chooser.Controller,
                    storeSelected = "chosenCard",
                    prompt = "Choose a nonland card to exile",
                ),
                MoveCollectionEffect(
                    from = "chosenCard",
                    destination = CardDestination.ToZone(Zone.EXILE, Player.ContextPlayer(0)),
                ),
            ),
        ).then(Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, creature))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "70"
        artist = "Ovidio Cartagena"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/993ade84-031f-4a3e-bd68-55f61b559248.jpg?1743204240"
    }
}
