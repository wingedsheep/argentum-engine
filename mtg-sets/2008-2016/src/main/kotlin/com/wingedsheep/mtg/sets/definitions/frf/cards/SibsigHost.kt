package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sibsig Host
 * {4}{B}
 * Creature — Zombie
 * 2/6
 * When this creature enters, each player mills three cards.
 */
val SibsigHost = card("Sibsig Host") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 2
    toughness = 6
    oracleText = "When this creature enters, each player mills three cards."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.mill(3, EffectTarget.PlayerRef(Player.Each))
        description = "When this creature enters, each player mills three cards."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "82"
        artist = "Steve Prescott"
        flavorText = "\"They were your friends, your family, your clan. They want only to welcome you.\"\n—Tasigur, the Golden Fang"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/2791f1a5-bf29-48ac-8a41-88b7f97ede41.jpg?1783938694"
    }
}
