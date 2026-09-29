package com.wingedsheep.mtg.sets.definitions.s99.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val DakmorGhoul = card("Dakmor Ghoul") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    oracleText = "When this creature enters, target opponent loses 2 life and you gain 2 life."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.LoseLife(2, opponent) then Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "70"
        artist = "Dana Knutson"
        flavorText = "\"Cursed be the sickly forms that err from honest Nature's rule!\"\n—Alfred, Lord Tennyson, \"Locksley Hall\""
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5caeb415-0e90-4f5b-ada4-a46241cf5dda.jpg?1783946037"
    }
}
