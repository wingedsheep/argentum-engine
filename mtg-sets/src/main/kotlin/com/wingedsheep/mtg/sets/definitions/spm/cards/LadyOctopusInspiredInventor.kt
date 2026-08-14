package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Lady Octopus, Inspired Inventor
 * {U}
 * Legendary Creature — Human Scientist Villain
 * 0/2
 *
 * Whenever you draw your first or second card each turn, put an ingenuity counter on Lady Octopus.
 * {T}: You may cast an artifact spell from your hand with mana value less than or equal to the
 * number of ingenuity counters on Lady Octopus without paying its mana cost.
 *
 * The "first or second card each turn" clause is two [Triggers.NthCardDrawn] triggers — one for the
 * first draw, one for the second — each of which adds an [Counters.INGENUITY] counter to Lady
 * Octopus. A third (or later) draw advances no [Triggers.NthCardDrawn] and so adds no counter.
 *
 * The {T} ability is the standard gather → filter → choose-up-to-one → cast-without-paying pipeline
 * (as on Yue, the Moon Spirit / Kellan, the Kid): [GatherCardsEffect] pulls artifact cards from
 * hand, [FilterCollectionEffect] narrows them to mana value ≤ the live ingenuity-counter count on
 * Lady Octopus ([CollectionFilter.ManaValueAtMost] over `countersOnSelf(INGENUITY)`),
 * [SelectFromCollectionEffect] makes the "you may" choice (up to one), and
 * [Effects.CastFromCollectionWithoutPayingCost] casts the chosen artifact for free.
 */
val LadyOctopusInspiredInventor = card("Lady Octopus, Inspired Inventor") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Scientist Villain"
    power = 0
    toughness = 2
    oracleText = "Whenever you draw your first or second card each turn, put an ingenuity counter " +
        "on Lady Octopus.\n" +
        "{T}: You may cast an artifact spell from your hand with mana value less than or equal to " +
        "the number of ingenuity counters on Lady Octopus without paying its mana cost."

    triggeredAbility {
        trigger = Triggers.NthCardDrawn(1)
        effect = Effects.AddCounters(Counters.INGENUITY, 1, EffectTarget.Self)
        description = "Whenever you draw your first card each turn, put an ingenuity counter on Lady Octopus."
    }

    triggeredAbility {
        trigger = Triggers.NthCardDrawn(2)
        effect = Effects.AddCounters(Counters.INGENUITY, 1, EffectTarget.Self)
        description = "Whenever you draw your second card each turn, put an ingenuity counter on Lady Octopus."
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.Composite(
            GatherCardsEffect(
                CardSource.FromZone(Zone.HAND, filter = GameObjectFilter.Artifact),
                storeAs = "ladyOctopusCandidates",
            ),
            FilterCollectionEffect(
                from = "ladyOctopusCandidates",
                filter = CollectionFilter.ManaValueAtMost(
                    DynamicAmounts.countersOnSelf(CounterTypeFilter.Named(Counters.INGENUITY)),
                ),
                storeMatching = "ladyOctopusEligible",
            ),
            SelectFromCollectionEffect(
                from = "ladyOctopusEligible",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                storeSelected = "ladyOctopusChosen",
                selectedLabel = "Cast without paying its mana cost",
            ),
            Effects.CastFromCollectionWithoutPayingCost("ladyOctopusChosen"),
        )
        description = "You may cast an artifact spell from your hand with mana value less than or " +
            "equal to the number of ingenuity counters on Lady Octopus without paying its mana cost."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "35"
        artist = "Fariba Khamseh"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c5f360b-f9a0-46e0-9e8b-58e5b4b0389e.jpg?1783905354"
    }
}
