package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dark Remedy (M20 #321)
 * {1}{B}  Instant
 *
 * Target creature gets +1/+3 until end of turn.
 */
val DarkRemedy = card("Dark Remedy") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature gets +1/+3 until end of turn."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(1, 3, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "321"
        artist = "Svetlin Velinov"
        flavorText = "\"Writhing tendrils of darkness stitched him back together. Though his will and body both were broken, the necromancer's magic would not let him rest.\"\n—Krinnea, *Siege of the Bone Spire*"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c926d38-a741-47a9-8961-f5bcf2909939.jpg"
    }
}
