package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Panther Pounce
 * {W}
 * Instant
 * Target player investigates. Target creature gets +1/+0 and gains flying until end of turn. Untap it.
 *
 * Implementation note: two independent cast-time targets — a player (who gets the Clue) and a
 * creature. `Effects.Investigate(controller = ...)` routes the Clue token to the targeted player
 * rather than the spell's controller; the pump, flying grant and untap all read the creature target.
 */
val PantherPounce = card("Panther Pounce") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target player investigates. Target creature gets +1/+0 and gains flying until end of turn. Untap it. (To investigate, create a Clue token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")"
    spell {
        val player = target("player", TargetPlayer())
        val creature = target("creature", TargetCreature(filter = TargetFilter.Creature))
        effect = Effects.Composite(
            Effects.Investigate(1, controller = player),
            Effects.ModifyStats(1, 0, creature),
            Effects.GrantKeyword(Keyword.FLYING, creature),
            Effects.Untap(creature),
        )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "29"
        artist = "Le Vuong"
        flavorText = "\"Did you truly think you could run from me?\"\n—T'Challa, the Black Panther"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/4122ae40-800d-4b6b-93c9-aa503c33f1a9.jpg?1783902969"
    }
}
