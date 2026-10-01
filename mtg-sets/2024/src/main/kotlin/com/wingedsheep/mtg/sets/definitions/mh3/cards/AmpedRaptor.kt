package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Amped Raptor
 * {1}{R}
 * Creature — Dinosaur
 * 2/1
 *
 * First strike
 * When this creature enters, you get {E}{E} (two energy counters). Then if you cast it from your
 * hand, exile cards from the top of your library until you exile a nonland card. You may cast that
 * card by paying an amount of {E} equal to its mana value rather than paying its mana cost.
 *
 * The cast happens during the trigger's resolution ([Effects.CastFromCollectionByPaying]), so card
 * type timing is ignored and it can't be saved for later. The {E} is the spell's own cost, priced off
 * the card being cast (`DynamicAmounts.sourceManaValue()`) and charged by the cast handler. The
 * "you may" is only asked when the energy is there to pay with; anything not cast stays in exile.
 */
val AmpedRaptor = card("Amped Raptor") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dinosaur"
    power = 2
    toughness = 1
    oracleText = "First strike\n" +
        "When this creature enters, you get {E}{E} (two energy counters). Then if you cast it from your " +
        "hand, exile cards from the top of your library until you exile a nonland card. You may cast that " +
        "card by paying an amount of {E} equal to its mana value rather than paying its mana cost."

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            run(Effects.GetEnergy(2))
            run(Effects.If(
                Conditions.WasCastFromHand,
                Effects.Pipeline {
                    val (nonland, exiled) = gatherUntilMatch(GameObjectFilter.Nonland)
                    reveal(exiled)
                    exile(exiled)
                    run(Effects.If(
                        Conditions.CompareAmounts(
                            DynamicAmounts.energyCount(),
                            ComparisonOperator.GTE,
                            DynamicAmounts.manaValueOf(nonland),
                        ),
                        Effects.May(
                            Effects.CastFromCollectionByPaying(
                                nonland,
                                Costs.additional.PayPlayerCounters(
                                    CounterType.ENERGY, DynamicAmounts.sourceManaValue()
                                ),
                            ),
                        ),
                    ))
                },
            ))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "114"
        artist = "Alex Konstad"
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1ac0e78b-0fdd-44f9-8b7b-c4f28a32782e.jpg?1783911274"

        ruling(
            "2024-06-07",
            "Any land cards exiled with Amped Raptor's triggered ability will remain in exile. If you choose " +
                "not to cast the exiled nonland card (either because you don't have enough {E} or you just " +
                "don't want to), that card will remain in exile as well."
        )
        ruling(
            "2024-06-07",
            "You choose whether or not to cast the exiled nonland card as Amped Raptor's triggered ability " +
                "resolves. If you do, you do so as part of the resolution of that ability. You can't wait to " +
                "cast it later in the turn. Timing restrictions based on the card's type are ignored."
        )
        ruling(
            "2024-06-07",
            "If you cast a spell for another cost \"rather than paying its mana cost,\" you can't choose to " +
                "cast it for any alternative costs. You can, however, pay additional costs, such as kicker " +
                "costs. If the spell has any mandatory additional costs, those must be paid to cast it."
        )
    }
}
