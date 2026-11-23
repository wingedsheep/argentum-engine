package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Biovisionary — Gatecrash #146
 * {1}{G}{U} · Creature — Human Wizard · 2/3
 *
 * At the beginning of the end step, if you control four or more creatures named Biovisionary,
 * you win the game.
 *
 * Test of Endurance's intervening-"if" win shape on every player's end step, counting creatures
 * by name so token copies count too.
 */
val Biovisionary = card("Biovisionary") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 3
    oracleText = "At the beginning of the end step, if you control four or more creatures named " +
        "Biovisionary, you win the game."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        interveningIf = Conditions.YouControlAtLeast(4, GameObjectFilter.Creature.named("Biovisionary"))
        effect = Effects.WinGame()
        description = "At the beginning of the end step, if you control four or more creatures named " +
            "Biovisionary, you win the game."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "146"
        artist = "Ryan Barger"
        flavorText = "He tired of his fellow researchers and their useless prattling, so he decided to make " +
            "a network of scholars with whom he could carry a conversation."
        imageUri = "https://cards.scryfall.io/normal/front/2/0/2000b4e8-7887-454e-9d52-211516613dd0.jpg?1783940112"
        ruling("2013-01-24", "Token creatures that are a copy of Biovisionary will count.")
        ruling("2013-01-24", "If you don't control four or more creatures named Biovisionary at the beginning of the end step, the ability won't trigger.")
        ruling("2013-01-24", "If the ability does trigger, but you don't control four or more creatures named Biovisionary when the ability tries to resolve, the ability will do nothing.")
    }
}
