package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate

/**
 * Walking Skyscraper — Kamigawa: Neon Dynasty #263 (canonical printing)
 * {8} · Artifact Creature — Construct · 8/8
 *
 * This spell costs {1} less to cast for each modified creature you control. Trample. This creature
 * has hexproof as long as it's untapped.
 *
 * The reduction counts creatures you control matching [StatePredicate.IsModified] (CR 700.9), the
 * same filter [TowashiGuideBot] uses for its activation discount; the hexproof is Paradise Druid's
 * untapped-conditional self grant.
 */
val WalkingSkyscraper = card("Walking Skyscraper") {
    manaCost = "{8}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    power = 8
    toughness = 8
    oracleText = "This spell costs {1} less to cast for each modified creature you control. " +
        "(Equipment, Auras you control, and counters are modifications.)\n" +
        "Trample\n" +
        "This creature has hexproof as long as it's untapped. " +
        "(It can't be the target of spells or abilities your opponents control.)"

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.PermanentsYouControlMatching(
                    GameObjectFilter.Creature.withStatePredicate(StatePredicate.IsModified)
                )
            )
        )
    }

    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.HEXPROOF, GroupFilter.source()),
            condition = Conditions.SourceIsUntapped
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "263"
        artist = "David Auden Nash"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/958af4ba-f29e-47ef-995e-3985982c75ad.jpg?1783923819"
    }
}
