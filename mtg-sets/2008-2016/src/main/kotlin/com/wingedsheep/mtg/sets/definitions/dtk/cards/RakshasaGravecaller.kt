package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.exploit
import com.wingedsheep.sdk.model.Rarity

/**
 * Rakshasa Gravecaller
 * {4}{B}
 * Creature — Demon
 * 3/6
 * Exploit (When this creature enters, you may sacrifice a creature.)
 * When this creature exploits a creature, create two 2/2 black Zombie creature tokens.
 *
 * The token payoff rides the exploit reflexive. Like Necromaster Dragon's Zombie, the token carries
 * no explicit image — its art is resolved from the set's token art.
 */
val RakshasaGravecaller = card("Rakshasa Gravecaller") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    power = 3
    toughness = 6
    oracleText = "Exploit (When this creature enters, you may sacrifice a creature.)\n" +
        "When this creature exploits a creature, create two 2/2 black Zombie creature tokens."

    exploit(
        onExploit = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            count = 2
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "114"
        artist = "Jakub Kasper"
        flavorText = "Debts to rakshasa linger beyond death."
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0bf584fd-b13b-4f86-9c9a-fa6514255aba.jpg"
    }
}
