package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Spire Mechcycle — Aetherdrift #147
 * {4}{R} · Artifact — Vehicle · 5/4
 *
 * Haste
 * Exhaust — Tap another untapped Mount or Vehicle you control: This Vehicle becomes an artifact
 * creature. Put a +1/+1 counter on it for each Mount and/or Vehicle you control other than this
 * Vehicle. (Activate each exhaust ability only once.)
 * Crew 2
 *
 * Same shape as Marshals' Pathcruiser — no "until end of turn" clause on the animate, so
 * [Duration.Permanent] with the printed 5/4 as the base P/T and the counters layering on top in
 * 7d. The cost's "untapped … you control" is intrinsic to `CostAtom.TapPermanents` (the engine
 * offers only controlled untapped candidates), so the filter carries the subtype test alone.
 *
 * The permanent tapped to pay the cost is still on the battlefield when the ability resolves, so
 * it counts toward the counters — which is why the count is taken at resolution, not at activation.
 */
private val MountOrVehicle = GameObjectFilter(
    cardPredicates = listOf(
        CardPredicate.Or(
            listOf(
                CardPredicate.HasSubtype(Subtype("Mount")),
                CardPredicate.HasSubtype(Subtype.VEHICLE)
            )
        )
    )
)

val SpireMechcycle = card("Spire Mechcycle") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Vehicle"
    power = 5
    toughness = 4
    oracleText = "Haste\n" +
        "Exhaust — Tap another untapped Mount or Vehicle you control: This Vehicle becomes an " +
        "artifact creature. Put a +1/+1 counter on it for each Mount and/or Vehicle you control " +
        "other than this Vehicle. (Activate each exhaust ability only once.)\n" +
        "Crew 2 (Tap any number of creatures you control with total power 2 or more: This Vehicle " +
        "becomes an artifact creature until end of turn.)"

    keywords(Keyword.HASTE)

    activatedAbility {
        cost = Costs.TapAnotherPermanent(MountOrVehicle)
        isExhaust = true
        effect = Effects.Composite(
            Effects.BecomeCreature(
                target = EffectTarget.Self,
                power = 5,
                toughness = 4,
                duration = Duration.Permanent
            ),
            Effects.AddDynamicCounters(
                Counters.PLUS_ONE_PLUS_ONE,
                DynamicAmount.AggregateBattlefield(
                    player = Player.You,
                    filter = MountOrVehicle,
                    excludeSelf = true
                ),
                EffectTarget.Self
            )
        )
        description = "Exhaust — Tap another untapped Mount or Vehicle you control: This Vehicle " +
            "becomes an artifact creature. Put a +1/+1 counter on it for each Mount and/or " +
            "Vehicle you control other than this Vehicle."
    }

    keywordAbility(KeywordAbility.crew(2))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "147"
        artist = "Adam Volker"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f483debe-9c54-4323-aec5-226587ece2c9.jpg?1783907876"

        ruling(
            "2025-02-07",
            "If an exhaust ability of a permanent is activated, and then that permanent leaves the " +
                "battlefield and returns to the battlefield, it becomes a new object so its " +
                "exhaust ability can be activated again."
        )
    }
}
