package com.wingedsheep.mtg.sets.definitions.tsp.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Swarmyard — Time Spiral #278
 * Land
 *
 * {T}: Add {C}.
 * {T}: Regenerate target Insect, Rat, Spider, or Squirrel.
 *
 * The target names four creature types and no card type, so it is any permanent with one of them
 * (a Kindred permanent qualifies too) — a [GameObjectFilter.Permanent] with a `withAnySubtype` union.
 */
val Swarmyard = card("Swarmyard") {
    typeLine = "Land"
    colorIdentity = ""
    oracleText = "{T}: Add {C}.\n" +
        "{T}: Regenerate target Insect, Rat, Spider, or Squirrel. " +
        "(The next time it would be destroyed this turn, instead tap it, remove it from combat, and heal all damage on it.)"

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        val creature = target(
            TargetFilter(GameObjectFilter.Permanent.withAnySubtype("Insect", "Rat", "Spider", "Squirrel")),
        )
        effect = Effects.Regenerate(creature)
        description = "{T}: Regenerate target Insect, Rat, Spider, or Squirrel."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "278"
        artist = "Thomas M. Baxa"
        flavorText = "Where scavengers nest in hollowed-out rib cages and chittering crawlers peer from empty eye sockets, few dare to trespass."
        imageUri = "https://cards.scryfall.io/normal/front/2/5/25bfe849-a508-4fa8-8999-909fe8ec34fa.jpg?1783943193"
    }
}
