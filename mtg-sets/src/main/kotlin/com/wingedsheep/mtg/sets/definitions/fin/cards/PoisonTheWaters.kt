package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.RevealHandEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Poison the Waters
 * {1}{B}
 * Sorcery
 *
 * Choose one —
 * • All creatures get -1/-1 until end of turn.
 * • Target player reveals their hand. You choose an artifact or creature card from it.
 *   That player discards that card.
 */
val PoisonTheWaters = card("Poison the Waters") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n• All creatures get -1/-1 until end of turn.\n• Target player reveals their hand. You choose an artifact or creature card from it. That player discards that card."

    spell {
        modal(chooseCount = 1) {
            mode("All creatures get -1/-1 until end of turn") {
                effect = Effects.ForEachInGroup(
                    filter = GroupFilter.AllCreatures,
                    effect = ModifyStatsEffect(-1, -1, EffectTarget.Self)
                )
            }
            mode("Target player reveals their hand; discard an artifact or creature card") {
                val t = target("target", TargetPlayer())
                effect = Effects.Composite(
                    listOf(
                        RevealHandEffect(t),
                        GatherCardsEffect(
                            source = CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)),
                            storeAs = "hand"
                        ),
                        SelectFromCollectionEffect(
                            from = "hand",
                            selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                            chooser = Chooser.Controller,
                            filter = GameObjectFilter.Artifact or GameObjectFilter.Creature,
                            storeSelected = "toDiscard",
                            prompt = "Choose an artifact or creature card to discard",
                            alwaysPrompt = true,
                            showAllCards = true
                        ),
                        MoveCollectionEffect(
                            from = "toDiscard",
                            destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                            moveType = MoveType.Discard
                        )
                    )
                )
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "111"
        artist = "Arif Wijaya"
        flavorText = "\"Hee-hee... Nothing beats the sweet music of hundreds of voices screaming in unison! Uwee-hee-hee!\""
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff2bafe7-4d0f-464d-b7ba-55a54366fc68.jpg?1748706178"
    }
}
