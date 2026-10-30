package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Finale of Devastation — War of the Spark #160
 * {X}{G}{G} · Sorcery
 *
 * Search your library and/or graveyard for a creature card with mana value X or less and put it
 * onto the battlefield. If you search your library this way, shuffle. If X is 10 or more,
 * creatures you control get +X/+X and gain haste until end of turn.
 *
 * The search is [Patterns.Library.searchLibraryAndOrGraveyard]: a choice of library, graveyard
 * or both, shuffling only when the library was searched. The X ≥ 10 rider runs after the search, so the creature just put onto the battlefield
 * gets +X/+X and haste too (ruling), and it applies even when nothing was found. Pump and haste
 * are one pass over one group, as the sentence names its group once.
 */
val FinaleOfDevastation = card("Finale of Devastation") {
    manaCost = "{X}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library and/or graveyard for a creature card with mana value X or less " +
        "and put it onto the battlefield. If you search your library this way, shuffle. If X is 10 " +
        "or more, creatures you control get +X/+X and gain haste until end of turn."

    spell {
        effect = Patterns.Library.searchLibraryAndOrGraveyard(
            filter = GameObjectFilter.Creature.manaValueAtMostDynamic(DynamicAmounts.xValue()),
            destination = SearchDestination.BATTLEFIELD,
        ) then Effects.If(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.xValue(),
                ComparisonOperator.GTE,
                10,
            ),
            then = Effects.ForEachInGroup(
                GroupFilter.AllCreaturesYouControl,
                Effects.ModifyStats(DynamicAmounts.xValue(), DynamicAmounts.xValue(), EffectTarget.IterationEntity) then
                    Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity)
            ),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "160"
        artist = "Bayard Wu"
        flavorText = "Some spells sing a quiet tune. Others roar."
        imageUri = "https://cards.scryfall.io/normal/front/9/8/985453e7-997e-4d77-a338-cc0290791ebe.jpg?1783933415"
        ruling("2019-05-03", "If a creature card in your library or graveyard has {X} in its mana cost, X is considered to be 0.")
        ruling("2019-05-03", "If X is 10 or more, the creature card you just put onto the battlefield will get +X/+X and haste.")
        ruling("2019-05-03", "If you don't find a creature card with mana value X or less, creatures you control still get +X/+X and gain haste if X is 10 or more.")
    }
}
