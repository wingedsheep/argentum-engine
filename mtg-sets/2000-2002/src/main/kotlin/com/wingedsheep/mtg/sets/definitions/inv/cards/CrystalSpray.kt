package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetSpellOrPermanent
import com.wingedsheep.sdk.scripting.effects.TextWordCategory

/**
 * Crystal Spray
 * {2}{U}
 * Instant
 * Change the text of target spell or permanent by replacing all instances of one
 * color word with another or one basic land type with another until end of turn.
 * Draw a card.
 *
 * Invasion engine gap #17: text-changing (color word / basic land type). Uses the new
 * [Effects.ChangeWordInText] primitive — the player chooses a color word or basic land type
 * to replace and its replacement at resolution; the change is recorded as an end-of-turn
 * [com.wingedsheep.engine.state.components.identity.TextReplacement] on the target.
 *
 * Because a basic-land-type change flows through the projected type line, a Forest swapped to
 * Island taps for {U} (mana follows from the projected subtype via IntrinsicManaAbilities),
 * gains Islandwalk relevance, etc.; a color-word change rewrites protection-from-color and
 * color filters (e.g. "nonred" -> "nonblue").
 */
val CrystalSpray = card("Crystal Spray") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Change the text of target spell or permanent by replacing all instances of one color word with another or one basic land type with another until end of turn.\nDraw a card."

    spell {
        val t = target(TargetSpellOrPermanent())
        effect = Effects.ChangeWordInText(TextWordCategory.entries.toSet(), t) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "50"
        artist = "Jeff Miracola"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/8798a4f1-34bb-449d-a8cc-faf8bda8e0ab.jpg?1562922403"
    }
}
