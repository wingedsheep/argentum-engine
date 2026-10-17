package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Elspeth Resplendent — Streets of New Capenna #11.
 * {3}{W}{W} · Legendary Planeswalker — Elspeth · Starting loyalty 5
 *
 * The +1 is a resolution-time [Effects.ChooseAction] over the four keyword-counter kinds (the
 * ruling: the kind is chosen first, then both counters land on the creature); each branch puts the
 * +1/+1 counter and the chosen keyword counter on the up-to-one target in the same resolution.
 *
 * The −3 is the look-at-the-top-seven dig as a pipeline: an optional pick of a permanent card with
 * mana value 3 or less, moved onto the battlefield with `addCounterType = SHIELD` so the shield
 * counter is there as it enters, and the rest bottomed in a random order.
 */
val ElspethResplendent = card("Elspeth Resplendent") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Planeswalker — Elspeth"
    startingLoyalty = 5
    oracleText = "+1: Choose up to one target creature. Put a +1/+1 counter and a counter from among " +
        "flying, first strike, lifelink, or vigilance on it.\n" +
        "−3: Look at the top seven cards of your library. You may put a permanent card with mana value " +
        "3 or less from among them onto the battlefield with a shield counter on it. Put the rest on the " +
        "bottom of your library in a random order.\n" +
        "−7: Create five 3/3 white Angel creature tokens with flying."

    loyaltyAbility(+1) {
        val creature = target(TargetFilter.Creature, optional = true)
        fun branch(label: String, kind: CounterType) = EffectChoice(
            label = label,
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
                Effects.AddCounters(kind, 1, creature),
        )
        effect = Effects.ChooseAction(
            listOf(
                branch("Flying counter", CounterType.FLYING),
                branch("First strike counter", CounterType.FIRST_STRIKE),
                branch("Lifelink counter", CounterType.LIFELINK),
                branch("Vigilance counter", CounterType.VIGILANCE),
            )
        )
    }

    loyaltyAbility(-3) {
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(7))
            val (kept, rest) = chooseUpToSplit(
                1,
                from = looked,
                filter = GameObjectFilter.Permanent.manaValueAtMost(3),
                prompt = "You may put a permanent card with mana value 3 or less onto the battlefield " +
                    "with a shield counter on it.",
                showAllCards = true,
            )
            move(kept, CardDestination.ToZone(Zone.BATTLEFIELD), addCounterType = CounterType.SHIELD)
            toLibraryBottom(rest, order = CardOrder.Random)
        }
    }

    loyaltyAbility(-7) {
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Angel"),
            keywords = setOf(Keyword.FLYING),
            count = 5,
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "11"
        artist = "Anna Steinbauer"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c606af0-f3d1-44a4-b24e-ee1263569b1f.jpg?1783923158"
        ruling("2022-04-29", "For Elspeth's first ability, its controller chooses flying, first strike, lifelink, or vigilance, then that counter and the +1/+1 counter are placed on the target creature at the same time.")
        ruling("2022-04-29", "Shield counters don't prevent players from sacrificing creatures.")
        ruling("2022-04-29", "Removing a shield counter in this way isn't the same as regenerating a creature.")
        ruling("2022-04-29", "If a permanent that would be dealt damage has more than one shield counter on it, that damage is prevented and only one shield counter is removed.")
        ruling("2022-04-29", "If a permanent with a shield counter is dealt unpreventable damage, that damage will be dealt and a shield counter will still be removed.")
        ruling("2022-04-29", "A creature with a shield counter on it may still be destroyed by state-based actions if it has damage marked on it equal to its toughness or has been dealt unpreventable damage by a source with deathtouch.")
        ruling("2022-04-29", "“Shield” is not an ability that creatures have and shield counters are not keyword counters. If a creature with a shield counter loses its abilities, the shield counter will still protect it as normal.")
    }
}
