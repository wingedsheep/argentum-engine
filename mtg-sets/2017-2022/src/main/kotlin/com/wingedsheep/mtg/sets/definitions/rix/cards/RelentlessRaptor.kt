package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.MustBlock

/**
 * Relentless Raptor — Rivals of Ixalan #169
 * {R}{W} · Creature — Dinosaur · 3/3
 *
 * Vigilance
 * This creature attacks or blocks each combat if able.
 *
 * "Attacks or blocks each combat if able" is the pair of requirements (as on Iron Golem):
 * it must attack in its controller's combats and must block in opponents' combats.
 */
val RelentlessRaptor = card("Relentless Raptor") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Dinosaur"
    power = 3
    toughness = 3
    oracleText = "Vigilance\nThis creature attacks or blocks each combat if able."

    keywords(Keyword.VIGILANCE)

    staticAbility {
        ability = MustAttack()
    }

    staticAbility {
        ability = MustBlock()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "169"
        artist = "Matt Stewart"
        flavorText = "As the battle for Orazca unfolded, the contenders discovered that others had already staked a claim."
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d0727ba-4180-4908-93be-a8abf29a8958.jpg?1783935271"
        ruling(
            "2018-01-19",
            "If the defending player somehow gains control of Relentless Raptor after it attacks, it must also block if able."
        )
    }
}
