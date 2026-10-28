package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Drana's Silencer — Zendikar Rising #99
 * {5}{B} · Creature — Vampire Rogue · 3/2
 *
 * When this creature enters, target creature an opponent controls gets -X/-X until end of turn,
 * where X is the number of creatures in your party.
 *
 * X is [DynamicAmounts.partySize] read on resolution; the Silencer counts itself as the Rogue if
 * it's still on the battlefield.
 */
val DranasSilencer = card("Drana's Silencer") {
    manaCost = "{5}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Rogue"
    oracleText = "When this creature enters, target creature an opponent controls gets -X/-X until end of turn, " +
        "where X is the number of creatures in your party. (Your party consists of up to one each of Cleric, " +
        "Rogue, Warrior, and Wizard.)"
    power = 3
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureOpponentControls)
        val negX = -DynamicAmounts.partySize()
        effect = Effects.ModifyStats(negX, negX, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "99"
        artist = "Mads Ahm"
        flavorText = "\"Hush.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59224f9a-deb2-4dd4-b2d4-967232c14254.jpg?1783929379"
        ruling(
            "2020-09-25",
            "The value of X is determined only as Drana's Silencer's ability resolves. Once that happens, the " +
                "value of X won't change later in the turn even if the number of creatures in your party changes."
        )
        ruling(
            "2020-09-25",
            "If an ability of a creature counts the number of creatures in your party, that number is counted " +
                "as the ability resolves. If that creature is still on the battlefield when the ability resolves, " +
                "it'll be counted if appropriate."
        )
    }
}
