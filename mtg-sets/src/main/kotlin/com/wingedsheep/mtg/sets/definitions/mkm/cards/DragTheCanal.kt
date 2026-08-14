package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect

/**
 * Drag the Canal — Murders at Karlov Manor #199
 * {U}{B} · Instant
 *
 * Create a 2/2 white and blue Detective creature token. If a creature died this turn, you gain 2
 * life, surveil 2, then investigate.
 *
 * Two mana always buys the Detective; the rest is a morbid rider. "If a creature died this turn"
 * is a resolution-time check on the whole game's turn history (any creature, any controller —
 * including one that died to combat damage in the same turn this instant is cast), so this is at
 * its best cast after blockers trade rather than pre-combat.
 *
 * The rider's three clauses are ordered as printed: gain 2, then surveil 2, then investigate. The
 * ordering matters when the surveil is what turns on something watching the graveyard, and it
 * means the Clue is created after any cards the surveil bins.
 *
 * The token's art comes from the MKM `tokenArt` layer (the 2/2 white-and-blue Detective is one of
 * the set's printed tokens), so no `imageUri` is baked in here.
 */
val DragTheCanal = card("Drag the Canal") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Instant"
    oracleText = "Create a 2/2 white and blue Detective creature token. If a creature died this " +
        "turn, you gain 2 life, surveil 2, then investigate. (Create a Clue token. It's an " +
        "artifact with \"{2}, Sacrifice this token: Draw a card.\")"

    spell {
        effect = Effects.Composite(
            Effects.CreateToken(
                power = 2,
                toughness = 2,
                colors = setOf(Color.WHITE, Color.BLUE),
                creatureTypes = setOf("Detective")
            ),
            ConditionalEffect(
                condition = Conditions.CreatureDiedThisTurn,
                effect = Effects.Composite(
                    Effects.GainLife(2),
                    Effects.Surveil(2),
                    Effects.Investigate()
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "199"
        artist = "Josh Hass"
        flavorText = "\"Let's gather what we can before the Golgari tidy up the scene.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/0/508d7096-2be3-4d4b-a55c-d4dbd3c9019c.jpg?1783912850"

        ruling(
            "2024-02-02",
            "Clue is an artifact type. Even though it appears on some cards with other permanent " +
                "types, it's never a creature type, a land type, or anything but an artifact type."
        )
        ruling(
            "2024-02-02",
            "Some abilities trigger \"whenever you sacrifice a Clue\". Those abilities trigger " +
                "whenever you sacrifice a Clue for any reason, not just to activate a Clue's " +
                "activated ability."
        )
    }
}
