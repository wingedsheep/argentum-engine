package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.storied
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Ori, Keeper of Songs
 * {2}{W}
 * Legendary Creature — Dwarf Bard
 * 3/3
 *
 * Storied.
 * As long as you have an enduring story, Ori gets +1/+0 and has vigilance.
 *
 * The plainest storied card in the set, and so the one that shows the shape: [storied] wires nothing
 * but the keyword, because handing out the enduring story is the engine's CR 702.195a state-based
 * action, and the payoff half is two ordinary continuous effects gated on
 * [Conditions.YouHaveEnduringStory].
 *
 * Two statics rather than one because they land in different Rule 613 layers — the +1/+0 in layer 7c
 * and the vigilance grant in layer 6 — and both re-evaluate every projection. Ori is himself
 * legendary, so he counts toward his own threshold: with two other artifacts/legendaries/Sagas out,
 * he turns himself on the moment he resolves.
 */
val OriKeeperOfSongs = card("Ori, Keeper of Songs") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Dwarf Bard"
    oracleText = "Storied (If you control three or more artifacts, legendaries, and/or Sagas, you " +
        "have an enduring story for the rest of the game.)\n" +
        "As long as you have an enduring story, Ori gets +1/+0 and has vigilance."
    power = 3
    toughness = 3

    storied()

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(powerBonus = 1, toughnessBonus = 0, filter = GroupFilter.source()),
            condition = Conditions.YouHaveEnduringStory
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter.source()),
            condition = Conditions.YouHaveEnduringStory
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "23"
        artist = "Yigit Koroglu"
        flavorText = "The music began all at once, so sudden and sweet that Bilbo forgot everything else."
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c5727af5-a487-4b16-8278-81c3c928c417.jpg?1785323179"
    }
}
