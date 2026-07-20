package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Banner of Kinship
 * {5}
 * Artifact
 *
 * As this artifact enters, choose a creature type. This artifact enters with a fellowship counter
 * on it for each creature you control of the chosen type.
 * Creatures you control of the chosen type get +1/+1 for each fellowship counter on this artifact.
 *
 * Both as-enters effects are replacement effects that apply while the Banner is entering, so their
 * order is fixed by the engine: [EntersWithChoice] resumes first and writes the chosen type onto
 * the entering entity's `CastChoicesComponent`, and only then does the enters-with-counters pass
 * evaluate `count`. That's what lets the dynamic count filter on `withChosenSubtype()`.
 *
 * The counter total is a *snapshot* taken on entry (CR 614.1c) — later creatures of the chosen type
 * do not add counters. The anthem, by contrast, is a live static ability sized by
 * [Counters.FELLOWSHIP] currently on the Banner, so removing counters shrinks it.
 */
val BannerOfKinship = card("Banner of Kinship") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "As this artifact enters, choose a creature type. This artifact enters with a " +
        "fellowship counter on it for each creature you control of the chosen type.\n" +
        "Creatures you control of the chosen type get +1/+1 for each fellowship counter on this artifact."

    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE))

    replacementEffect(
        EntersWithDynamicCounters(
            counterType = CounterTypeFilter.Named(Counters.FELLOWSHIP),
            count = DynamicAmount.AggregateBattlefield(
                Player.You,
                GameObjectFilter.Creature.withChosenSubtype()
            )
        )
    )

    staticAbility {
        val fellowship = DynamicAmounts.countersOnSelf(CounterTypeFilter.Named(Counters.FELLOWSHIP))
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.ChosenSubtypeCreatures().youControl(),
            powerBonus = fellowship,
            toughnessBonus = fellowship
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "127"
        artist = "Olena Richards"
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a14c16c0-4053-46b0-8fa6-be8b4a7a1c8a.jpg?1783909089"
    }
}
