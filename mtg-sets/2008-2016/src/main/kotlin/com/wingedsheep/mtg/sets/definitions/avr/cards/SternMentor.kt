package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.dsl.soulbond
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Stern Mentor — Avacyn Restored #77 (canonical printing)
 * {3}{U} · Creature — Human Wizard · 2/2
 *
 * Soulbond
 * As long as this creature is paired with another creature, each of those creatures has
 * "{T}: Target player mills two cards."
 *
 * [DeadeyeNavigator]'s shape carrying [EvanescentIntellect]'s granted mill:
 * [GrantActivatedAbility] over `GroupFilter.soulbondPair()`. The {T} cost taps whichever half
 * it is activated on, so that half's summoning sickness is what gates it.
 */
val SternMentor = card("Stern Mentor") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    oracleText = "Soulbond (You may pair this creature with another unpaired creature when either " +
        "enters. They remain paired for as long as you control both of them.)\n" +
        "As long as this creature is paired with another creature, each of those creatures has " +
        "\"{T}: Target player mills two cards.\""
    power = 2
    toughness = 2

    soulbond()

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Tap
                val player = target(Targets.Player)
                effect = Patterns.Library.mill(2, player)
                description = "{T}: Target player mills two cards."
            },
            filter = GroupFilter.soulbondPair()
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "77"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ffe4d34f-68f0-4d79-9aab-58c5304224d9.jpg?1783940710"
    }
}
