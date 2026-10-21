package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost

/**
 * Rimeshield Frost Giant
 * {3}{U}{U}
 * Creature — Giant Warrior
 * 4/5
 * Ward {3} (Whenever this creature becomes the target of a spell or ability an opponent controls, counter it unless that player pays {3}.)
 */
val RimeshieldFrostGiant = card("Rimeshield Frost Giant") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Giant Warrior"
    oracleText = "Ward {3} (Whenever this creature becomes the target of a spell or ability an opponent controls, counter it unless that player pays {3}.)"
    power = 4
    toughness = 5
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{3}")))
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "69"
        artist = "Matt Stewart"
        flavorText = "Frost giants carry the trophies of their greatest victories into battle, fashioned into armor of strength and savage beauty."
        imageUri = "https://cards.scryfall.io/normal/front/7/6/767b499e-3bdb-4809-b430-6e965bd100d4.jpg?1783926510"
    }
}
