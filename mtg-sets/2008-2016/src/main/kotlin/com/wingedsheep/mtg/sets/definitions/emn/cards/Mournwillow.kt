package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Mournwillow (Eldritch Moon #187)
 * {1}{B}{G}
 * Creature — Plant Skeleton
 * 3/2
 *
 * Haste
 * Delirium — When this creature enters, if there are four or more card types among cards in your
 * graveyard, creatures with power 2 or less can't block this turn.
 *
 * The can't-block is a floating group restriction (`CantBlockGroup`), not a snapshot: it changes no
 * characteristics, so the affected set is re-evaluated when blockers are declared (CR 611.2c) — a
 * creature pumped above 2 power can block, one shrunk to 2 or less can't, and creatures that enter
 * later in the turn are covered too. It affects every creature, including the controller's own.
 */
val Mournwillow = card("Mournwillow") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Plant Skeleton"
    power = 3
    toughness = 2
    oracleText = "Haste\n" +
        "Delirium — When this creature enters, if there are four or more card types among cards in " +
        "your graveyard, creatures with power 2 or less can't block this turn."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.Delirium(4)
        effect = Effects.CantBlockGroup(GroupFilter.AllCreatures.powerAtMost(2))
        description = "Delirium — When this creature enters, if there are four or more card types " +
            "among cards in your graveyard, creatures with power 2 or less can't block this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "187"
        artist = "Jeff Simpson"
        imageUri = "https://cards.scryfall.io/normal/front/4/5/45aa37d8-6765-443f-9b70-200fecf8fa1e.jpg?1783937428"
        ruling(
            "2016-07-13",
            "Because the last ability's effect doesn't change the characteristics of any permanents, " +
                "the set of creatures affected is constantly updated. Any creature with power 2 or less " +
                "at the moment blockers are chosen will be unable to block."
        )
    }
}
