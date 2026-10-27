package com.wingedsheep.mtg.sets.definitions.roe.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Awakening Zone — Rise of the Eldrazi #176
 * {2}{G} · Enchantment
 *
 * At the beginning of your upkeep, you may create a 0/1 colorless Eldrazi Spawn creature token.
 * It has "Sacrifice this token: Add {C}."
 */
val AwakeningZone = card("Awakening Zone") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, you may create a 0/1 colorless Eldrazi Spawn creature " +
        "token. It has \"Sacrifice this token: Add {C}.\""

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        optional = true
        effect = Effects.CreateEldraziSpawn(
            imageUri = "https://cards.scryfall.io/normal/front/e/0/e0e3826c-3c85-4910-bd6c-04894ea328d0.jpg?1783941948"
        )
        description = "At the beginning of your upkeep, you may create a 0/1 colorless Eldrazi Spawn " +
            "creature token. It has \"Sacrifice this token: Add {C}.\""
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "176"
        artist = "Johann Bodin"
        flavorText = "The ground erupted in time with the hedron's thrum, a dirge of the last days."
        imageUri = "https://cards.scryfall.io/normal/front/0/8/080dbd69-95a8-4fed-bbaf-875a8a34a2c9.jpg?1783941967"
    }
}
