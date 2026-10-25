package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.MustBlock

/**
 * Iron Golem
 * {4}
 * Artifact Creature — Golem
 * 5/3
 * Vigilance
 * This creature attacks or blocks each combat if able.
 *
 * "Attacks or blocks each combat if able" is the pair of requirements: it must attack in
 * its controller's combats and must block in opponents' combats (a creature never gets to
 * do both in one combat).
 */
val IronGolem = card("Iron Golem") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Golem"
    oracleText = "Vigilance\nThis creature attacks or blocks each combat if able."
    power = 5
    toughness = 3

    keywords(Keyword.VIGILANCE)

    staticAbility {
        ability = MustAttack()
    }

    staticAbility {
        ability = MustBlock()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "247"
        artist = "Nicholas Gregory"
        flavorText = "Once an iron golem is set to a task, it won't turn aside until that task is complete or it has been destroyed."
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5ae24ef5-b12c-48ee-935a-00e048fb8d0f.jpg?1783926437"
    }
}
