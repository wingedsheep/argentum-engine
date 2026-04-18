package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ChooseActionEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Thirst for Identity
 * {2}{U}
 * Instant
 *
 * Draw three cards. Then discard two cards unless you discard a creature card.
 *
 * Modeled as: Draw 3, then ChooseAction between "discard a creature card"
 * (feasible only if a creature card is in hand) and "discard two cards".
 */
val ThirstForIdentity = card("Thirst for Identity") {
    manaCost = "{2}{U}"
    typeLine = "Instant"
    oracleText = "Draw three cards. Then discard two cards unless you discard a creature card."

    spell {
        effect = Effects.DrawCards(3)
            .then(
                ChooseActionEffect(
                    choices = listOf(
                        EffectChoice(
                            label = "Discard a creature card",
                            effect = CompositeEffect(
                                listOf(
                                    GatherCardsEffect(
                                        source = CardSource.FromZone(
                                            Zone.HAND,
                                            Player.You,
                                            GameObjectFilter.Creature
                                        ),
                                        storeAs = "creatures"
                                    ),
                                    SelectFromCollectionEffect(
                                        from = "creatures",
                                        selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                                        storeSelected = "discarded",
                                        prompt = "Choose a creature card to discard"
                                    ),
                                    MoveCollectionEffect(
                                        from = "discarded",
                                        destination = CardDestination.ToZone(Zone.GRAVEYARD),
                                        moveType = MoveType.Discard
                                    )
                                )
                            ),
                            feasibilityCheck = FeasibilityCheck.HasCardsInZone(
                                Zone.HAND,
                                GameObjectFilter.Creature,
                                1
                            )
                        ),
                        EffectChoice(
                            label = "Discard two cards",
                            effect = EffectPatterns.discardCards(2)
                        )
                    )
                )
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "79"
        artist = "Danny Schwartz"
        flavorText = "Within each rimekin rages an ongoing struggle for self-discovery and purpose."
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3949f8c-d1c5-45c2-80ed-a57f4f9af86e.jpg?1767957051"
    }
}
