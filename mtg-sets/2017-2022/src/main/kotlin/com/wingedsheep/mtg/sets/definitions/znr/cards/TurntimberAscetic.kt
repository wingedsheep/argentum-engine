package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Turntimber Ascetic — Zendikar Rising #214
 * {4}{G} · Creature — Giant Cleric · 5/4
 *
 * When this creature enters, you gain 3 life.
 */
val TurntimberAscetic = card("Turntimber Ascetic") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Giant Cleric"
    oracleText = "When this creature enters, you gain 3 life."
    power = 5
    toughness = 4

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(3)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "214"
        artist = "Nicholas Gregory"
        flavorText = "\"For so long, this land cried out in pain. Now, as the flow returns to its balance point, " +
            "a new harmony echoes in the trees.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b33b6922-2584-43e1-98d4-e722e7c9393c.jpg"
    }
}
