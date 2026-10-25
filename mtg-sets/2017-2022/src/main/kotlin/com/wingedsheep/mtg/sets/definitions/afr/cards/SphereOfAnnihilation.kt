package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sphere of Annihilation
 * {X}{B}
 * Artifact
 *
 * This artifact enters with X void counters on it.
 * At the beginning of your upkeep, exile this artifact, all creatures and planeswalkers with mana
 * value less than or equal to the number of void counters on it, and all creature and planeswalker
 * cards in graveyards with mana value less than or equal to the number of void counters on it.
 *
 * The void count is snapshotted once with `storeNumber` before anything moves, so both filters
 * read the same number and exiling the Sphere itself can't change it. Every collection is
 * gathered before the first exile, so the three moves act on the objects that were there when
 * the ability resolved.
 */
val SphereOfAnnihilation = card("Sphere of Annihilation") {
    manaCost = "{X}{B}"
    colorIdentity = "B"
    typeLine = "Artifact"
    oracleText = "This artifact enters with X void counters on it.\n" +
        "At the beginning of your upkeep, exile this artifact, all creatures and planeswalkers with " +
        "mana value less than or equal to the number of void counters on it, and all creature and " +
        "planeswalker cards in graveyards with mana value less than or equal to the number of void " +
        "counters on it."

    replacementEffect(
        EntersWithDynamicCounters(
            counterType = CounterType.VOID,
            count = DynamicAmounts.xValue()
        )
    )

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Pipeline {
            val voids = storeNumber(DynamicAmounts.countersOnSelf(CounterType.VOID), name = "voids")
            val matching = GameObjectFilter(
                cardPredicates = listOf(
                    CardPredicate.Or(listOf(CardPredicate.IsCreature, CardPredicate.IsPlaneswalker)),
                    CardPredicate.ManaValueAtMostDynamic(voids.amount)
                )
            )
            val sphere = gather(CardSource.Entity(EffectTarget.Self))
            val permanents = gather(matching, excludeSelf = true)
            val graveyardCards = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.Each, matching))
            exile(sphere)
            exile(permanents)
            exile(graveyardCards)
        }
        description = "At the beginning of your upkeep, exile this artifact, all creatures and planeswalkers " +
            "with mana value less than or equal to the number of void counters on it, and all creature and " +
            "planeswalker cards in graveyards with mana value less than or equal to the number of void " +
            "counters on it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "121"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d2315a9-584b-4c59-af43-dd38bcb6c322.jpg?1783926488"
    }
}
