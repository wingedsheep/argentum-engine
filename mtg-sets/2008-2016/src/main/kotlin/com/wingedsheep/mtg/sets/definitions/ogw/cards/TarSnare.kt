package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Tar Snare
 * {2}{B}
 * Instant
 * Target creature gets -3/-2 until end of turn.
 */
val TarSnare = card("Tar Snare") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature gets -3/-2 until end of turn."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(-3, -2, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "90"
        artist = "Karl Kopinski"
        flavorText = "The amorphous, magically animated residue known as the Fathom Sludge lurks in the Hagra Cistern, attacking its prey with pseudopods and dragging it down to be digested."
        imageUri = "https://cards.scryfall.io/normal/front/0/6/06ee017c-25ae-4668-8cfe-ae3b55e851aa.jpg"
    }
}
