package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Temple Altisaur — Rivals of Ixalan #28
 * {4}{W}
 * Creature — Dinosaur
 * 3/4
 * If a source would deal damage to another Dinosaur you control, prevent all but 1 of that damage.
 *
 * A standing prevention ([PreventDamage.allBut]), not a cap: damage that can't be prevented is dealt
 * in full, and it never protects the Altisaur itself ("another"). Two Altisaurs protect each other.
 */
val TempleAltisaur = card("Temple Altisaur") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dinosaur"
    power = 3
    toughness = 4
    oracleText = "If a source would deal damage to another Dinosaur you control, prevent all but 1 of that damage."

    replacementEffect(
        PreventDamage(
            allBut = 1,
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.Object(
                    GameObjectFilter.Creature.withSubtype("Dinosaur").youControl().notSourceItself()
                )
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "28"
        artist = "Daarken"
        flavorText = "\"Kinjalli sculpted humans from clay, but the dinosaurs were carved from unyielding stone.\"\n—Itzama the Crested"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa8f8d61-51d6-479b-a812-6cbacc7ea1fc.jpg?1783935330"
    }
}
