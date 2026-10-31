package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rimrock Knight // Boulder Rush
 * {1}{R}
 * Creature — Dwarf Knight
 * 3/1
 * This creature can't block.
 *
 * Adventure: Boulder Rush — {R}, Instant — Adventure
 * Target creature gets +2/+0 until end of turn.
 */
val RimrockKnight = card("Rimrock Knight") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dwarf Knight"
    power = 3
    toughness = 1
    oracleText = "This creature can't block."

    staticAbility {
        ability = CantBlock()
    }

    adventure("Boulder Rush") {
        manaCost = "{R}"
        typeLine = "Instant — Adventure"
        oracleText = "Target creature gets +2/+0 until end of turn. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.ModifyStats(2, 0, t)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "137"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a3d13d84-01e4-4429-93db-e5afff811527.jpg?1783932619"
    }
}
