package com.wingedsheep.mtg.sets.definitions.ecl.cards

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
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Perfect Intimidation
 * {3}{B}
 * Sorcery
 *
 * Choose one or both —
 * • Target opponent exiles two cards from their hand.
 * • Remove all counters from target creature.
 *
 * Mode 1 is modeled with the gather/select/move pipeline; the targeted opponent
 * (Chooser.TargetPlayer) selects which cards to exile from their own hand. If
 * they have fewer than two cards, the SelectFromCollection executor exiles all
 * eligible cards instead of stalling.
 */
val PerfectIntimidation = card("Perfect Intimidation") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose one or both —\n" +
        "• Target opponent exiles two cards from their hand.\n" +
        "• Remove all counters from target creature."

    val opponentExilesTwo = Effects.Composite(
        listOf(
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0), GameObjectFilter.Any),
                storeAs = "handCards"
            ),
            SelectFromCollectionEffect(
                from = "handCards",
                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(2)),
                chooser = Chooser.TargetPlayer,
                storeSelected = "exiledCards",
                prompt = "Exile two cards from your hand"
            ),
            MoveCollectionEffect(
                from = "exiledCards",
                destination = CardDestination.ToZone(Zone.EXILE, Player.ContextPlayer(0))
            )
        )
    )

    spell {
        modal(chooseCount = 2, minChooseCount = 1) {
            mode("Target opponent exiles two cards from their hand") {
                target("opponent", Targets.Opponent)
                effect = opponentExilesTwo
            }
            mode("Remove all counters from target creature") {
                target("creature", Targets.Creature)
                effect = Effects.RemoveAllCounters(EffectTarget.ContextTarget(0))
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "115"
        artist = "Heather Hudson"
        flavorText = "Morcant's voice was as sweet as honey and as dangerous as moonglove."
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff05b7f4-e0d0-4304-99b3-66ac804129fe.jpg?1767970136"
    }
}
