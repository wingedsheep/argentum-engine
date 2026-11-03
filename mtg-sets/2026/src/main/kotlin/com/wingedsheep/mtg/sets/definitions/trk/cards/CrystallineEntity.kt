package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Crystalline Entity — Star Trek #261
 * {8} · Artifact Creature — Elemental · 7/7 · Mythic
 *
 * Flying
 * When this creature enters, if you cast it, destroy all nonartifact creatures.
 *
 * The intervening-if is [Conditions.WasCast] (any zone, unlike Reiver Demon's from-hand form), so
 * reanimating or blinking the Entity gets a 7/7 flier and no wipe. The Entity is an artifact, so it
 * sits outside its own `Creature.nonartifact()` filter and survives the wipe.
 */
val CrystallineEntity = card("Crystalline Entity") {
    manaCost = "{8}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Elemental"
    power = 7
    toughness = 7
    oracleText = "Flying\n" +
        "When this creature enters, if you cast it, destroy all nonartifact creatures."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasCast
        effect = Effects.DestroyAll(GameObjectFilter.Creature.nonartifact())
        description = "When this creature enters, if you cast it, destroy all nonartifact creatures."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "261"
        artist = "David Álvarez"
        flavorText = "\"Ironic, isn't it? That something so unique could care so little for the " +
            "diversity of life.\"\n—Captain Jean-Luc Picard"
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cbc4ac70-0a4c-4973-a366-dd67d8901e21.jpg?1784036804"
    }
}
