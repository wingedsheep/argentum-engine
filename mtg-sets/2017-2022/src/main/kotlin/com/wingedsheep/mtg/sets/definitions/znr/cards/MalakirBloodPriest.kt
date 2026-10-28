package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Malakir Blood-Priest — Zendikar Rising #110
 * {1}{B} · Creature — Vampire Cleric · 2/1
 *
 * When this creature enters, each opponent loses X life and you gain X life, where X is the number
 * of creatures in your party.
 *
 * X is [DynamicAmounts.partySize] read on resolution; the Blood-Priest counts itself as the Cleric
 * if it's still on the battlefield.
 */
val MalakirBloodPriest = card("Malakir Blood-Priest") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Cleric"
    oracleText = "When this creature enters, each opponent loses X life and you gain X life, where X is the " +
        "number of creatures in your party. (Your party consists of up to one each of Cleric, Rogue, Warrior, " +
        "and Wizard.)"
    power = 2
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.LoseLife(DynamicAmounts.partySize(), EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.GainLife(DynamicAmounts.partySize())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "110"
        artist = "Scott Murphy"
        flavorText = "\"From your veins to mine.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d14ea81-d303-47db-90b4-7d10588f7ea6.jpg?1783929373"
        ruling(
            "2020-09-25",
            "In a Two-Headed Giant game, Malakir Blood-Priest's ability causes the opposing team to lose twice X " +
                "life and you to gain X life."
        )
        ruling(
            "2020-09-25",
            "If an ability of a creature counts the number of creatures in your party, that number is counted " +
                "as the ability resolves. If that creature is still on the battlefield when the ability resolves, " +
                "it'll be counted if appropriate."
        )
    }
}
