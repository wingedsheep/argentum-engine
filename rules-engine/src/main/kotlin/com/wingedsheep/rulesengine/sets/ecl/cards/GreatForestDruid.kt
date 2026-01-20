package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.AddAnyColorManaEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Great Forest Druid
 *
 * {1}{G} Creature — Treefolk Druid 0/4
 * {T}: Add one mana of any color.
 */
object GreatForestDruid {
    val definition = CardDefinition.creature(
        name = "Great Forest Druid",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.TREEFOLK, Subtype.of("Druid")),
        power = 0,
        toughness = 4,
        oracleText = "{T}: Add one mana of any color.",
        metadata = ScryfallMetadata(
            collectorNumber = "178",
            rarity = Rarity.COMMON,
            artist = "Pete Venters",
            imageUri = "https://cards.scryfall.io/normal/front/d/d/ddeeff12-4567-8901-defg-ddeeff124567.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Great Forest Druid") {
        // {T}: Add one mana of any color
        manaAbility(AddAnyColorManaEffect())
    }
}
