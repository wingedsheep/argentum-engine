package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Kor Celebrant — Zendikar Rising #22
 * {2}{W} · Creature — Kor Cleric · 1/4
 *
 * Whenever this creature or another creature you control enters, you gain 1 life.
 *
 * "This creature or another" is the ANY binding over creatures you control, so the Celebrant's
 * own arrival triggers it too.
 */
val KorCelebrant = card("Kor Celebrant") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Kor Cleric"
    oracleText = "Whenever this creature or another creature you control enters, you gain 1 life."
    power = 1
    toughness = 4

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.GainLife(1)
        description = "Whenever this creature or another creature you control enters, you gain 1 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "22"
        artist = "Jake Murray"
        flavorText = "\"The landbind ritual connects us to our sacred sites. More importantly, it binds us " +
            "with every other kor who has visited here, through countless generations.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87c37e46-a961-4739-8893-f783013e2be6.jpg"
    }
}
