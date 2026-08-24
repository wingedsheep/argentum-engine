package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Ghostly Visit
 * {2}{B}
 * Sorcery
 * Destroy target nonblack creature.
 */
val GhostlyVisit = card("Ghostly Visit") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy target nonblack creature."

    spell {
        val t = target("target", TargetCreature(filter = TargetFilter.Creature.notColor(Color.BLACK)))
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "76"
        artist = "Kang Yu"
        flavorText = "Cao Cao was haunted to death by the ghosts of the empresses and other high courtiers he had murdered."
        imageUri = "https://cards.scryfall.io/normal/front/0/6/06f6938a-229a-4521-b5d5-7999ce5fb372.jpg"
    }
}
