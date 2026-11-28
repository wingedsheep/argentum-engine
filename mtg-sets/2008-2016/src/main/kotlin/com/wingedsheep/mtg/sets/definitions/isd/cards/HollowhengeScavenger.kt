package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Hollowhenge Scavenger
 * {3}{G}{G}
 * Creature — Elemental
 * 4/5
 * Morbid — When this creature enters, if a creature died this turn, you gain 5 life.
 */
val HollowhengeScavenger = card("Hollowhenge Scavenger") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental"
    oracleText = "Morbid — When this creature enters, if a creature died this turn, you gain 5 life."
    power = 4
    toughness = 5
    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.CreatureDiedThisTurn
        effect = Effects.GainLife(5)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "188"
        artist = "Slawomir Maniak"
        flavorText = "After werewolves slaughtered the citizenry of Hollowhenge, other creatures moved in."
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c9ff632-0e27-4521-9e9d-5725e618f5dd.jpg"
    }
}
