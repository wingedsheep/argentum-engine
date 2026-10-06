package com.wingedsheep.mtg.sets.definitions.mbs.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

// Oracle gives this originally Construct-only creature the Phyrexian subtype.
val PierceStrider = card("Pierce Strider") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Phyrexian Construct"
    oracleText = "When this creature enters, target opponent loses 3 life."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.LoseLife(3, opponent)
        description = "When this creature enters, target opponent loses 3 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "123"
        artist = "Igor Kieryluk"
        flavorText = "\"Pain isn't a negative stimulus. Pain is a sign of your imperfection.\"\n—Sheoldred, Whispering One"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88b449a5-634f-47b1-a757-86a6849f6777.jpg?1783941365"
    }
}
