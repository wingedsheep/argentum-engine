package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Thundering Sparkmage — Zendikar Rising #171
 * {3}{R} · Creature — Human Wizard · 2/2
 *
 * When this creature enters, it deals X damage to target creature or planeswalker, where X is the
 * number of creatures in your party.
 *
 * X is [DynamicAmounts.partySize] read on resolution; the Sparkmage counts itself as the Wizard if
 * it's still on the battlefield.
 */
val ThunderingSparkmage = card("Thundering Sparkmage") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Wizard"
    oracleText = "When this creature enters, it deals X damage to target creature or planeswalker, where X is " +
        "the number of creatures in your party. (Your party consists of up to one each of Cleric, Rogue, " +
        "Warrior, and Wizard.)"
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.DealDamage(DynamicAmounts.partySize(), t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "171"
        artist = "Billy Christian"
        flavorText = "\"Stay away from my friends!\""
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a9e6de80-3516-426b-8a1b-bda45f8c0cac.jpg?1783929347"
        ruling(
            "2020-09-25",
            "If an ability of a creature counts the number of creatures in your party, that number is counted " +
                "as the ability resolves. If that creature is still on the battlefield when the ability resolves, " +
                "it'll be counted if appropriate."
        )
    }
}
