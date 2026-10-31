package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Embereth Shieldbreaker // Battle Display
 * {1}{R}
 * Creature — Human Knight
 * 2/1
 *
 * Adventure: Battle Display — {R}, Sorcery — Adventure
 * Destroy target artifact.
 */
val EmberethShieldbreaker = card("Embereth Shieldbreaker") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Knight"
    oracleText = ""
    power = 2
    toughness = 1

    adventure("Battle Display") {
        manaCost = "{R}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Destroy target artifact. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            val t = target(TargetFilter.Artifact)
            effect = Effects.Destroy(t)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "122"
        artist = "Randy Vargas"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6cc73d16-5ed7-4104-91f6-0997a2080e2e.jpg?1783932628"
    }
}
