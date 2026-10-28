package com.wingedsheep.mtg.sets.definitions.nph.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyMaximumHandSize
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Jin-Gitaxias, Core Augur
 * {8}{U}{U}
 * Legendary Creature — Phyrexian Praetor
 * 5/4
 * Flash
 * At the beginning of your end step, draw seven cards.
 * Each opponent's maximum hand size is reduced by seven.
 */
val JinGitaxiasCoreAugur = card("Jin-Gitaxias, Core Augur") {
    manaCost = "{8}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Phyrexian Praetor"
    oracleText = "Flash\n" +
        "At the beginning of your end step, draw seven cards.\n" +
        "Each opponent's maximum hand size is reduced by seven."
    power = 5
    toughness = 4

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.DrawCards(7)
    }

    staticAbility {
        ability = ModifyMaximumHandSize(player = Player.EachOpponent, amount = DynamicAmounts.fixed(-7))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "37"
        artist = "Eric Deschamps"
        flavorText = "\"It is not a goal, but a process—the process of creating the perfect Phyrexia.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd46fc9f-5b92-44d7-8940-2f39b0962b8f.jpg?1783941319"
    }
}
