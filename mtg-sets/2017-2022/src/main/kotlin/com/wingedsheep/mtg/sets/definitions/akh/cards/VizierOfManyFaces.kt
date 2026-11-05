package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.embalm
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalCopyExceptions
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.effects.CopyExceptions

/**
 * Vizier of Many Faces — Amonkhet #74
 * {2}{U}{U} · Creature — Shapeshifter Cleric · Rare
 * 0/0
 *
 * You may have this creature enter as a copy of any creature on the battlefield, except if this
 * creature was embalmed, the token has no mana cost, it's white, and it's a Zombie in addition to
 * its other types.
 * Embalm {3}{U}{U}
 *
 * The embalmed token is a copy of this card, so it has the as-enters copy choice too. Entering as
 * a copy overwrites embalm's own exceptions, which is why the card restates them behind
 * "if this creature was embalmed" (CR 702.128b) — `Conditions.SourceWasEmbalmed`.
 */
val VizierOfManyFaces = card("Vizier of Many Faces") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Shapeshifter Cleric"
    power = 0
    toughness = 0
    oracleText = "You may have this creature enter as a copy of any creature on the battlefield, except if " +
        "this creature was embalmed, the token has no mana cost, it's white, and it's a Zombie in addition " +
        "to its other types.\n" +
        "Embalm {3}{U}{U} ({3}{U}{U}, Exile this card from your graveyard: Create a token that's a copy of it, " +
        "except it's a white Zombie Shapeshifter Cleric with no mana cost. Embalm only as a sorcery.)"

    replacementEffect(
        EntersAsCopy(
            optional = true,
            conditionalExceptions = listOf(
                ConditionalCopyExceptions(
                    condition = Conditions.SourceWasEmbalmed,
                    exceptions = CopyExceptions(
                        noManaCost = true,
                        overrideColors = setOf(Color.WHITE),
                        addedSubtypes = setOf(Subtype("Zombie")),
                    ),
                )
            ),
        )
    )

    embalm("{3}{U}{U}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "74"
        artist = "Ryan Yee"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2781c44-aa90-4a37-a812-33c643ba4deb.jpg?1783936515"
    }
}
