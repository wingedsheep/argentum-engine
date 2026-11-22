package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unleash
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Exava, Rakdos Blood Witch — Dragon's Maze #69 (canonical printing)
 * {2}{B}{R} · Legendary Creature — Human Cleric · 3/3
 *
 * First strike, haste
 * Unleash (You may have this creature enter with a +1/+1 counter on it. It can't block as long as
 * it has a +1/+1 counter on it.)
 * Each other creature you control with a +1/+1 counter on it has haste.
 */
val ExavaRakdosBloodWitch = card("Exava, Rakdos Blood Witch") {
    manaCost = "{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Human Cleric"
    power = 3
    toughness = 3
    oracleText = "First strike, haste\n" +
        "Unleash (You may have this creature enter with a +1/+1 counter on it. It can't block as long as it has a +1/+1 counter on it.)\n" +
        "Each other creature you control with a +1/+1 counter on it has haste."

    keywords(Keyword.FIRST_STRIKE, Keyword.HASTE)
    unleash()

    staticAbility {
        ability = GrantKeyword(
            Keyword.HASTE,
            GroupFilter(
                GameObjectFilter.Creature.youControl().withCounter(CounterType.PLUS_ONE_PLUS_ONE),
                excludeSelf = true,
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "69"
        artist = "Aleksi Briclot"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6cb72a64-89e7-4b0e-a3d3-1309829071d2.jpg?1783940029"
        ruling(
            "2013-04-15",
            "You make the choice to have the creature with unleash enter the battlefield with a +1/+1 counter or not as it's entering the battlefield. At that point, it's too late for a player to respond to the creature spell by trying to counter it, for example."
        )
        ruling("2013-04-15", "The unleash ability applies no matter where the creature is entering the battlefield from.")
        ruling(
            "2013-04-15",
            "A creature with unleash can't block if it has any +1/+1 counter on it, not just one put on it by the unleash ability."
        )
        ruling(
            "2013-04-15",
            "Putting a +1/+1 counter on a creature with unleash that's already blocking won't remove it from combat. It will continue to block."
        )
    }
}
