package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
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
 * Binding Negotiation
 * {1}{B}
 * Sorcery
 *
 * Target opponent reveals their hand. You may choose a nonland card from it. If you do,
 * they discard it. Otherwise, you may put a face-up exiled card they own into their
 * graveyard.
 *
 * Modeled entirely from atomic pipeline primitives:
 *  - Reveal → gather the opponent's hand → `ChooseUpTo(1)` nonland (the "you may choose")
 *    → discard. The optional selection stores `toDiscard`; an empty selection means no
 *    card was chosen.
 *  - The "Otherwise" half is a resolution-time state test ([ConditionalEffect], lowering to
 *    `Gate.WhenCondition`) gated on `toDiscard` being empty — i.e. nothing was discarded.
 *    It gathers the opponent's *face-up* exiled cards, offers an optional choice, and moves
 *    the chosen card to its owner's (the opponent's) graveyard.
 */
val BindingNegotiation = card("Binding Negotiation") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You may choose a nonland card from it. " +
        "If you do, they discard it. Otherwise, you may put a face-up exiled card they own into their graveyard."

    spell {
        val opponent = target("target opponent", TargetOpponent())
        effect = Effects.Composite(
            listOf(
                // Reveal the opponent's hand.
                RevealHandEffect(opponent),
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)),
                    storeAs = "opponentHand"
                ),
                // "You may choose a nonland card from it." — optional (ChooseUpTo 1).
                SelectFromCollectionEffect(
                    from = "opponentHand",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    chooser = Chooser.Controller,
                    filter = GameObjectFilter.Nonland,
                    storeSelected = "toDiscard",
                    prompt = "You may choose a nonland card for them to discard",
                    alwaysPrompt = true,
                    showAllCards = true
                ),
                // "If you do, they discard it."
                MoveCollectionEffect(
                    from = "toDiscard",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                    moveType = MoveType.Discard
                ),
                // "Otherwise, you may put a face-up exiled card they own into their graveyard."
                // Fires only if no card was discarded (toDiscard is empty).
                ConditionalEffect(
                    condition = Conditions.Not(Conditions.CollectionContainsMatch("toDiscard")),
                    effect = Effects.Composite(
                        listOf(
                            GatherCardsEffect(
                                source = CardSource.FromZone(
                                    zone = Zone.EXILE,
                                    player = Player.ContextPlayer(0),
                                    filter = GameObjectFilter.Any.faceUp()
                                ),
                                storeAs = "theirExile"
                            ),
                            SelectFromCollectionEffect(
                                from = "theirExile",
                                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                                chooser = Chooser.Controller,
                                storeSelected = "toBin",
                                prompt = "You may put a face-up exiled card they own into their graveyard",
                                alwaysPrompt = true,
                                showAllCards = true
                            ),
                            MoveCollectionEffect(
                                from = "toBin",
                                destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                                moveType = MoveType.Default
                            )
                        )
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "78"
        artist = "Caroline Gariba"
        flavorText = "\"The tighter I bind them, the looser their tongues become.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c4c26b9-981f-47cf-b0f4-769e788d9537.jpg?1712355546"
    }
}
