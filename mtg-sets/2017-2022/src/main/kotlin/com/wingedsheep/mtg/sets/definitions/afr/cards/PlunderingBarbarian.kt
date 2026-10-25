package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Plundering Barbarian
 * {2}{R}
 * Creature — Dwarf Barbarian
 * 2/2
 * When this creature enters, choose one —
 * • Smash the Chest — Destroy target artifact.
 * • Pry It Open — Create a Treasure token. (It's an artifact with "{T}, Sacrifice this token: Add one mana of any color.")
 */
val PlunderingBarbarian = card("Plundering Barbarian") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dwarf Barbarian"
    oracleText = "When this creature enters, choose one —\n" +
        "• Smash the Chest — Destroy target artifact.\n" +
        "• Pry It Open — Create a Treasure token. (It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"
    power = 2
    toughness = 2
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            mode("Smash the Chest — Destroy target artifact.") {
                val artifact = target(TargetFilter.Artifact)
                effect = Effects.Destroy(artifact)
            },
            Mode.noTarget(Effects.CreateTreasure(), "Pry It Open — Create a Treasure token."),
        )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "158"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d875881c-c285-47f1-8a37-e4a0239d47ec.jpg?1783926473"
    }
}
