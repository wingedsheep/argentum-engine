package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ChooseActionEffect
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Winternight Stories — Tarkir: Dragonstorm #67
 * {2}{U} · Sorcery · Rare
 *
 * Draw three cards. Then discard two cards unless you discard a creature card.
 * Harmonize {4}{U}
 *
 * "Discard two cards unless you discard a creature card" is a player choice (CR 701.8): the
 * controller may instead discard a single creature card to satisfy the "unless". Modeled as
 * Draw 3, then [ChooseActionEffect] between "discard a creature card" — feasible only when a
 * creature card is in hand — and "discard two cards". With no creature card in hand, only the
 * "discard two cards" branch is feasible, matching the printed card.
 */
val WinternightStories = card("Winternight Stories") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Draw three cards. Then discard two cards unless you discard a creature card.\n" +
        "Harmonize {4}{U} (You may cast this card from your graveyard for its harmonize cost. " +
        "You may tap a creature you control to reduce that cost by {X}, where X is its power. " +
        "Then exile this spell.)"

    spell {
        effect = Effects.DrawCards(3)
            .then(
                ChooseActionEffect(
                    choices = listOf(
                        EffectChoice(
                            label = "Discard a creature card",
                            effect = Effects.Pipeline {
                                val creatures = gather(
                                    CardSource.FromZone(
                                        Zone.HAND,
                                        Player.You,
                                        GameObjectFilter.Creature
                                    ),
                                    name = "creatures"
                                )
                                val discarded = chooseExactly(
                                    1, from = creatures,
                                    prompt = "Choose a creature card to discard",
                                    name = "discarded"
                                )
                                move(
                                    discarded,
                                    destination = CardDestination.ToZone(Zone.GRAVEYARD),
                                    moveType = MoveType.Discard
                                )
                            },
                            feasibilityCheck = FeasibilityCheck.HasCardsInZone(
                                Zone.HAND,
                                GameObjectFilter.Creature,
                                1
                            )
                        ),
                        EffectChoice(
                            label = "Discard two cards",
                            effect = Patterns.Hand.discardCards(2)
                        )
                    )
                )
            )
    }

    keywordAbility(KeywordAbility.harmonize("{4}{U}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "67"
        artist = "Zara Alfonso"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64d9367c-f50c-4568-aa63-6760c44ecaeb.jpg?1743204229"
    }
}
