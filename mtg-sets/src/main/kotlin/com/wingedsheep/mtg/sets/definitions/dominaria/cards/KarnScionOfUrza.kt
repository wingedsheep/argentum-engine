package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.StaticTarget
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Karn, Scion of Urza - {4}
 * Legendary Planeswalker - Karn
 * Starting Loyalty: 5
 *
 * +1: Reveal the top two cards of your library. An opponent chooses one of them.
 *     Put that card into your hand and exile the other with a silver counter on it.
 *
 * -1: Put a card you own with a silver counter on it from exile into your hand.
 *
 * -2: Create a 0/0 colorless Construct artifact creature token with
 *     "This creature gets +1/+1 for each artifact you control."
 */
val KarnScionOfUrza = card("Karn, Scion of Urza") {
    manaCost = "{4}"
    typeLine = "Legendary Planeswalker — Karn"
    startingLoyalty = 5
    oracleText = "+1: Reveal the top two cards of your library. An opponent chooses one of them. Put that card into your hand and exile the other with a silver counter on it.\n\u22121: Put a card you own with a silver counter on it from exile into your hand.\n\u22122: Create a 0/0 colorless Construct artifact creature token with \"This creature gets +1/+1 for each artifact you control.\""

    // +1: Reveal top 2, opponent chooses 1 for hand, other exiled with silver counter
    loyaltyAbility(+1) {
        effect = CompositeEffect(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(2)),
                    storeAs = "revealed",
                    revealed = true
                ),
                SelectFromCollectionEffect(
                    from = "revealed",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    chooser = Chooser.Opponent,
                    storeSelected = "chosen",
                    storeRemainder = "rest"
                ),
                MoveCollectionEffect(
                    from = "chosen",
                    destination = CardDestination.ToZone(Zone.HAND)
                ),
                MoveCollectionEffect(
                    from = "rest",
                    destination = CardDestination.ToZone(Zone.EXILE),
                    addCounterType = CounterType.SILVER
                )
            )
        )
    }

    // -1: Put a card with silver counter from exile into hand
    loyaltyAbility(-1) {
        effect = CompositeEffect(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(
                        zone = Zone.EXILE,
                        player = Player.You,
                        filter = GameObjectFilter.Any.withCounter(Counters.SILVER)
                    ),
                    storeAs = "silverExiled"
                ),
                SelectFromCollectionEffect(
                    from = "silverExiled",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    storeSelected = "chosen",
                    storeRemainder = "rest"
                ),
                MoveCollectionEffect(
                    from = "chosen",
                    destination = CardDestination.ToZone(Zone.HAND)
                )
            )
        )
    }

    // -2: Create 0/0 Construct artifact creature token with dynamic P/T
    loyaltyAbility(-2) {
        effect = CreateTokenEffect(
            power = 0,
            toughness = 0,
            colors = emptySet(),
            creatureTypes = setOf("Construct"),
            artifactToken = true,
            staticAbilities = listOf(
                GrantDynamicStatsEffect(
                    target = StaticTarget.SourceCreature,
                    powerBonus = DynamicAmount.AggregateBattlefield(
                        Player.You,
                        GameObjectFilter.Artifact
                    ),
                    toughnessBonus = DynamicAmount.AggregateBattlefield(
                        Player.You,
                        GameObjectFilter.Artifact
                    )
                )
            ),
            imageUri = "https://cards.scryfall.io/normal/front/c/5/c5eafa38-5333-4ef2-9661-08074c580a32.jpg?1562702317"
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "1"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/07a3d9e8-8597-498b-869c-cff79e0df516.jpg?1562730952"
        ruling("2018-04-27", "If Karn's first ability reveals fewer than two cards, the opponent still chooses one of them and the other is exiled with a silver counter on it.")
        ruling("2018-04-27", "A card you own in exile with a silver counter on it means any card, not just one exiled by Karn.")
        ruling("2018-04-27", "The Construct token gets +1/+1 for each artifact its controller controls, including itself.")
    }
}
