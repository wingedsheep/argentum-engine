package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Instrument of the Bards
 * {G}
 * Legendary Artifact
 *
 * At the beginning of your upkeep, you may put a harmony counter on Instrument of the Bards.
 * {3}{G}, {T}: Search your library for a creature card with mana value equal to the number of
 * harmony counters on Instrument of the Bards, reveal it, and put it into your hand. If that card
 * is legendary, create a Treasure token. Then shuffle.
 *
 * The search reads the harmony count as it resolves, and the Treasure check reads the found card
 * itself (`ifNotEmpty(found, legendary)`), so finding nothing makes no Treasure.
 */
val InstrumentOfTheBards = card("Instrument of the Bards") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Legendary Artifact"
    oracleText = "At the beginning of your upkeep, you may put a harmony counter on Instrument of the Bards.\n" +
        "{3}{G}, {T}: Search your library for a creature card with mana value equal to the number of " +
        "harmony counters on Instrument of the Bards, reveal it, and put it into your hand. If that card " +
        "is legendary, create a Treasure token. Then shuffle."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        optional = true
        effect = Effects.AddCounters(CounterType.HARMONY, 1, EffectTarget.Self)
        description = "At the beginning of your upkeep, you may put a harmony counter on Instrument of the Bards."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{G}"), Costs.Tap)
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(
                    Zone.LIBRARY,
                    Player.You,
                    GameObjectFilter.Creature and GameObjectFilter(
                        cardPredicates = listOf(
                            CardPredicate.ManaValueEqualsDynamic(DynamicAmounts.countersOnSelf(CounterType.HARMONY))
                        )
                    )
                ),
                search = true
            )
            val found = chooseUpTo(
                1,
                from = searchable,
                prompt = "Search your library for a creature card with mana value equal to the number of harmony counters",
            )
            move(found, CardDestination.ToZone(Zone.HAND, Player.You), revealed = true)
            ifNotEmpty(found, filter = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsLegendary))) {
                run(Effects.CreateTreasure(1))
            }
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
        }
        description = "{3}{G}, {T}: Search your library for a creature card with mana value equal to the number " +
            "of harmony counters on Instrument of the Bards, reveal it, and put it into your hand. If that card " +
            "is legendary, create a Treasure token. Then shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "190"
        artist = "Randy Gallegos"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d58486dc-3e78-4649-89c7-46f6210ff80f.jpg?1783926461"
    }
}
