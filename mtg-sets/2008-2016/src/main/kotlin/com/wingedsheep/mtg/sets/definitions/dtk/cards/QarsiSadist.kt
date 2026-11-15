package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.exploit
import com.wingedsheep.sdk.model.Rarity

/**
 * Qarsi Sadist
 * {1}{B}
 * Creature — Human Cleric
 * 1/3
 * Exploit (When this creature enters, you may sacrifice a creature.)
 * When this creature exploits a creature, target opponent loses 2 life and you gain 2 life.
 *
 * The targeted payoff rides the exploit reflexive, so the opponent is chosen after the sacrifice
 * resolves (as with Fell Stinger).
 */
val QarsiSadist = card("Qarsi Sadist") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 3
    oracleText = "Exploit (When this creature enters, you may sacrifice a creature.)\n" +
        "When this creature exploits a creature, target opponent loses 2 life and you gain 2 life."

    exploit {
        val opponent = target(Targets.Opponent)
        effect = Effects.LoseLife(2, opponent) then Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "113"
        artist = "Volkan Baǵa"
        flavorText = "Dying for the greater good still hurts."
        imageUri = "https://cards.scryfall.io/normal/front/0/6/068c44b9-0ba8-4275-b73d-18ad366f4b45.jpg"
    }
}
