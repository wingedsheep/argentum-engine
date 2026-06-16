package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Outlaws' Fury
 * {2}{R}
 * Instant
 *
 * Creatures you control get +2/+0 until end of turn. If you control an outlaw, exile the
 * top card of your library. Until the end of your next turn, you may play that card.
 * (Assassins, Mercenaries, Pirates, Rogues, and Warlocks are outlaws.)
 *
 * The team pump uses [Patterns.Group.modifyStatsForAll]. "If you control an outlaw" gates
 * the impulse-draw with [Conditions.YouControl] over [Filters.OutlawCreature]; the impulse
 * itself mirrors Season of the Bold — exile the top card, then grant
 * [GrantMayPlayFromExileEffect] with [MayPlayExpiry.UntilEndOfNextTurn].
 */
val OutlawsFury = card("Outlaws' Fury") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Creatures you control get +2/+0 until end of turn. If you control an outlaw, " +
        "exile the top card of your library. Until the end of your next turn, you may play that " +
        "card. (Assassins, Mercenaries, Pirates, Rogues, and Warlocks are outlaws.)"

    spell {
        effect = Effects.Composite(
            listOf(
                // Creatures you control get +2/+0 until end of turn.
                Patterns.Group.modifyStatsForAll(2, 0, Filters.Group.creaturesYouControl),
                // If you control an outlaw, exile the top card and let it be played until your next turn ends.
                ConditionalEffect(
                    Conditions.YouControl(Filters.OutlawCreature),
                    Effects.Composite(
                        listOf(
                            GatherCardsEffect(
                                source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
                                storeAs = "exiledCard",
                            ),
                            MoveCollectionEffect(
                                from = "exiledCard",
                                destination = CardDestination.ToZone(Zone.EXILE),
                            ),
                            GrantMayPlayFromExileEffect("exiledCard", MayPlayExpiry.UntilEndOfNextTurn),
                        )
                    ),
                ),
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "136"
        artist = "Diego Gisbert"
        flavorText = "The only witness to their rampage was the stoic visage of the moon."
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7502b9c-b759-499a-8e94-22f87f5eb142.jpg?1712355807"
    }
}
