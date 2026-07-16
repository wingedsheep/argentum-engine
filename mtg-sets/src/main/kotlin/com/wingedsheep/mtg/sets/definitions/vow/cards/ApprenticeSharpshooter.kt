package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.training
import com.wingedsheep.sdk.model.Rarity

/**
 * Apprentice Sharpshooter
 * {2}{G}
 * Creature — Human Archer
 * 1/4
 * Reach
 * Training (Whenever this creature attacks with another creature with greater power, put a
 * +1/+1 counter on this creature.)
 *
 * A vanilla-plus Training body: an unrelated evasion-denial keyword ([Keyword.REACH]) plus
 * [training], which installs the TRAINING keyword badge and the attack trigger (a +1/+1 counter
 * when it attacks alongside a creature of strictly greater projected power). Its low starting
 * power (1) makes it train readily; the high toughness (4) keeps it alive to keep training.
 */
val ApprenticeSharpshooter = card("Apprentice Sharpshooter") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Archer"
    power = 1
    toughness = 4
    oracleText = "Reach\n" +
        "Training (Whenever this creature attacks with another creature with greater power, " +
        "put a +1/+1 counter on this creature.)"

    keywords(Keyword.REACH)
    training()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "185"
        artist = "Steve Prescott"
        flavorText = "Innistrad's best archers are trained by the Quiver of Kessig, an order of " +
            "cathars who specialize in ranged combat."
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f03f0790-cdc9-4bb4-ae54-2c248435b0a4.jpg?1783924821"
    }
}
