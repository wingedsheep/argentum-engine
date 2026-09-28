package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Zodiac Dragon
 * {7}{R}{R}
 * Creature — Dragon
 * 8/8
 * When this creature is put into your graveyard from the battlefield, you may return it to your hand.
 */
val ZodiacDragon = card("Zodiac Dragon") {
    manaCost = "{7}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 8
    toughness = 8
    oracleText = "When this creature is put into your graveyard from the battlefield, you may return it to your hand."

    triggeredAbility {
        trigger = Triggers.self.dies()
        optional = true
        effect = Effects.Move(EffectTarget.Self, Zone.HAND, fromZone = Zone.GRAVEYARD)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "131"
        artist = "Qi Baocheng"
        flavorText = "\". . . The kingdoms three are now the stuff of dream, / For men to ponder, past all praise or blame.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/6/46652ae3-6572-4296-939b-0789923180d5.jpg?1783946101"
    }
}
