package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Gild
 * {3}{B}
 * Sorcery
 *
 * Exile target creature. Create a Gold token. (It's an artifact with "Sacrifice this token: Add one
 * mana of any color.")
 *
 * The Gold token goes to Gild's controller, whoever controlled the exiled creature. If the target is
 * illegal on resolution the spell doesn't resolve, so no Gold.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val Gild = card("Gild") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Exile target creature. Create a Gold token. " +
        "(It's an artifact with \"Sacrifice this token: Add one mana of any color.\")"

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.Exile(t) then Effects.CreateGold()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "73"
        artist = "Richard Wright"
        flavorText = "Merchants of the Underworld trade in coins of clay. Gold serves another purpose."
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8ce5e6c4-02c8-4602-96d6-19dad34f7804.jpg?1783939557"
        ruling("2014-02-01", "You will control the token, no matter who controls the target creature.")
        ruling("2014-02-01", "If the creature isn't a legal target as Gild tries to resolve, the spell won't resolve and none of its effects will happen. No token will be created.")
    }
}
