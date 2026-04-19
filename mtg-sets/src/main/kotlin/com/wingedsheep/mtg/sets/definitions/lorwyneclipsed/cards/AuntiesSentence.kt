package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.RevealHandEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Auntie's Sentence
 * {1}{B}
 * Sorcery
 *
 * Choose one —
 * • Target opponent reveals their hand. You choose a nonland permanent card from it.
 *   That player discards that card.
 * • Target creature gets -2/-2 until end of turn.
 */
val AuntiesSentence = card("Auntie's Sentence") {
    manaCost = "{1}{B}"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Target opponent reveals their hand. You choose a nonland permanent card from it. That player discards that card.\n" +
        "• Target creature gets -2/-2 until end of turn."

    spell {
        modal(chooseCount = 1) {
            mode("Target opponent reveals their hand, discard a nonland permanent card") {
                val opponent = target("target opponent", TargetOpponent())
                effect = CompositeEffect(
                    listOf(
                        RevealHandEffect(opponent),
                        GatherCardsEffect(
                            source = CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)),
                            storeAs = "hand"
                        ),
                        SelectFromCollectionEffect(
                            from = "hand",
                            selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                            chooser = Chooser.Controller,
                            filter = GameObjectFilter.NonlandPermanent,
                            storeSelected = "toDiscard",
                            prompt = "Choose a nonland permanent card to discard",
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
            mode("Target creature gets -2/-2 until end of turn") {
                val creature = target("target creature", Targets.Creature)
                effect = Effects.ModifyStats(-2, -2, creature)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "85"
        artist = "Vincent Christiaens"
        flavorText = "\"Stop whining, elf. The rest of us can see that it's *perfectly* fair, heh!\""
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e64bfe16-7362-4982-9136-1f4e0d335441.jpg?1767871817"
    }
}
