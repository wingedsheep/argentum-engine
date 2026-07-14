package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.training
import com.wingedsheep.sdk.model.Rarity

/**
 * Gryff Rider
 * {2}{W}
 * Creature — Human Knight
 * 2/1
 * Flying
 * Training (Whenever this creature attacks with another creature with greater power, put a
 * +1/+1 counter on this creature.)
 *
 * The baseline Training card: an unrelated evasion keyword plus [training], which installs the
 * TRAINING keyword badge and the attack trigger (a +1/+1 counter when it attacks alongside a
 * creature of strictly greater projected power). Its low starting power (2) makes it train easily.
 */
val GryffRider = card("Gryff Rider") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 1
    oracleText = "Flying\n" +
        "Training (Whenever this creature attacks with another creature with greater power, " +
        "put a +1/+1 counter on this creature.)"

    keywords(Keyword.FLYING)
    training()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "15"
        artist = "Yongjae Choi"
        flavorText = "\"Keep your heels down and bend at the hips as your mount takes flight. " +
            "She'll do the rest.\"\n—Anders, cathar drillmaster"
        imageUri = "https://cards.scryfall.io/normal/front/5/3/5317077b-370e-41a9-9162-c713362fd7f4.jpg?1783924922"
    }
}
