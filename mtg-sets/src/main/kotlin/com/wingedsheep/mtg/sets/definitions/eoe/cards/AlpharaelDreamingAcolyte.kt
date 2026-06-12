package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ChooseActionEffect
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Alpharael, Dreaming Acolyte
 * {1}{U}{B}
 * Legendary Creature — Human Cleric
 * 2/3
 *
 * When Alpharael enters, draw two cards. Then discard two cards unless you discard an artifact card.
 * During your turn, Alpharael has deathtouch.
 */
val AlpharaelDreamingAcolyte = card("Alpharael, Dreaming Acolyte") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Human Cleric"
    oracleText = "When Alpharael enters, draw two cards. Then discard two cards unless you discard an artifact card.\nDuring your turn, Alpharael has deathtouch."
    power = 2
    toughness = 3

    // ETB: draw two cards, then discard two cards unless you discard an artifact card
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.DrawCards(2)
            .then(
                ChooseActionEffect(
                    choices = listOf(
                        EffectChoice(
                            label = "Discard an artifact card",
                            effect = Effects.Pipeline {
                                val artifacts = gather(
                                    CardSource.FromZone(
                                        Zone.HAND,
                                        Player.You,
                                        GameObjectFilter.Artifact
                                    ),
                                    name = "artifacts"
                                )
                                val discarded = chooseExactly(
                                    1, from = artifacts,
                                    prompt = "Choose an artifact card to discard",
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
                                GameObjectFilter.Artifact,
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

    // Conditional deathtouch during your turn
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.DEATHTOUCH, GroupFilter.source()),
            condition = Conditions.IsYourTurn
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "212"
        artist = "Cristi Balanescu"
        flavorText = "\"I am worthy of the Faller's blessing. I can prove it.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/4/349a2211-2b23-418d-a1ef-1c72ad2e171d.jpg?1752947420"
    }
}
