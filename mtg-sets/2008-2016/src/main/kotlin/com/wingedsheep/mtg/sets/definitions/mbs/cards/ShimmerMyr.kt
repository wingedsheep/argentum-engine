package com.wingedsheep.mtg.sets.definitions.mbs.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType

/**
 * Shimmer Myr — Mirrodin Besieged #129
 * {3} · Artifact Creature — Myr 2/2
 *
 * Flash
 * You may cast artifact spells as though they had flash.
 *
 * Vedalken Orrery's controller-scoped [GrantFlashToSpellType], narrowed to artifact spells. It only
 * affects casting, so artifact lands are untouched (ruling).
 */
val ShimmerMyr = card("Shimmer Myr") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Myr"
    power = 2
    toughness = 2
    oracleText = "Flash\nYou may cast artifact spells as though they had flash."

    keywords(Keyword.FLASH)

    staticAbility {
        ability = GrantFlashToSpellType(
            filter = GameObjectFilter.Artifact,
            controllerOnly = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "129"
        artist = "Jana Schirmer & Johannes Voss"
        flavorText = "It evades Phyrexians by hiding in the spaces between seconds."
        imageUri = "https://cards.scryfall.io/normal/front/4/7/478c1a5b-237b-4ecb-be73-3c6dafd5ae53.jpg?1783941364"
        ruling("2020-11-10", "Once you announce that you're casting a spell, players can't attempt to remove Shimmer Myr from the battlefield to make that casting illegal. Removing Shimmer Myr after you've cast a spell won't affect that spell.")
        ruling("2020-11-10", "Shimmer Myr has no effect on artifact lands because they aren't cast as spells.")
    }
}
