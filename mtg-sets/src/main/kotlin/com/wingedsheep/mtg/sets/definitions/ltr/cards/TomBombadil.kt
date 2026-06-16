package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.GatherUntilMatchEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.RevealCollectionEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Tom Bombadil
 * {W}{U}{B}{R}{G}
 * Legendary Creature — God Bard
 * 4/4
 *
 * As long as there are four or more lore counters among Sagas you control, Tom Bombadil has
 * hexproof and indestructible.
 * Whenever the final chapter ability of a Saga you control resolves, reveal cards from the top
 * of your library until you reveal a Saga card. Put that card onto the battlefield and the rest
 * on the bottom of your library in a random order. This ability triggers only once each turn.
 *
 * The static gate uses [Conditions.CounterKindAmongYouControlAtLeast] to sum lore counters across
 * Sagas you control (CR 714 lore counters), and a ConditionalStaticAbility granting hexproof +
 * indestructible to itself. The trigger fires on [Triggers.WheneverFinalChapterOfYourSagaResolves]
 * (`oncePerTurn`) and composes GatherUntilMatch (until a Saga, from your library) + RevealCollection
 * + two filtered MoveCollections: the Saga → your battlefield, the rest → the bottom of your library
 * in random order.
 */
val TomBombadil = card("Tom Bombadil") {
    manaCost = "{W}{U}{B}{R}{G}"
    colorIdentity = "WUBRG"
    typeLine = "Legendary Creature — God Bard"
    power = 4
    toughness = 4
    oracleText = "As long as there are four or more lore counters among Sagas you control, Tom " +
        "Bombadil has hexproof and indestructible.\n" +
        "Whenever the final chapter ability of a Saga you control resolves, reveal cards from the " +
        "top of your library until you reveal a Saga card. Put that card onto the battlefield and " +
        "the rest on the bottom of your library in a random order. This ability triggers only " +
        "once each turn."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.HEXPROOF, GroupFilter.source()),
            condition = Conditions.CounterKindAmongYouControlAtLeast(
                count = 4,
                counterType = CounterTypeFilter.Named("lore"),
                filter = GameObjectFilter.Enchantment.withSubtype("Saga").youControl()
            )
        )
    }
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.INDESTRUCTIBLE, GroupFilter.source()),
            condition = Conditions.CounterKindAmongYouControlAtLeast(
                count = 4,
                counterType = CounterTypeFilter.Named("lore"),
                filter = GameObjectFilter.Enchantment.withSubtype("Saga").youControl()
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.WheneverFinalChapterOfYourSagaResolves
        oncePerTurn = true
        effect = Effects.Composite(
            listOf(
                GatherUntilMatchEffect(
                    player = Player.You,
                    filter = GameObjectFilter().withSubtype("Saga"),
                    storeMatch = "revealedSaga",
                    storeRevealed = "allRevealed"
                ),
                RevealCollectionEffect(from = "allRevealed"),
                // The Saga enters the battlefield under your control.
                MoveCollectionEffect(
                    from = "allRevealed",
                    filter = GameObjectFilter().withSubtype("Saga"),
                    destination = CardDestination.ToZone(
                        Zone.BATTLEFIELD,
                        player = Player.You
                    )
                ),
                // The rest go to the bottom of your library in a random order.
                MoveCollectionEffect(
                    from = "allRevealed",
                    filter = GameObjectFilter().notSubtype(Subtype("Saga")),
                    destination = CardDestination.ToZone(
                        Zone.LIBRARY,
                        player = Player.You,
                        placement = ZonePlacement.Bottom
                    ),
                    order = CardOrder.Random
                )
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "234"
        artist = "Dmitry Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2ab04c49-76a1-4896-8dca-8cb4c615f489.jpg?1686970104"
    }
}
