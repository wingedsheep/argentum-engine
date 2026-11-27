package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Moonsilver Spear
 * {4}
 * Artifact — Equipment
 * Equipped creature has first strike.
 * Whenever equipped creature attacks, create a 4/4 white Angel creature token with flying.
 * Equip {4}
 *
 * The Angel token's art resolves from Avacyn Restored's own `tokenArt`, so no `imageUri` is authored.
 */
val MoonsilverSpear = card("Moonsilver Spear") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature has first strike.\n" +
        "Whenever equipped creature attacks, create a 4/4 white Angel creature token with flying.\n" +
        "Equip {4}"

    staticAbility { ability = GrantKeyword(Keyword.FIRST_STRIKE, Filters.EquippedCreature) }

    triggeredAbility {
        trigger = Triggers.attached.attacks()
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Angel"),
            keywords = setOf(Keyword.FLYING),
        )
        description = "Whenever equipped creature attacks, create a 4/4 white Angel creature token with flying."
    }

    equipAbility("{4}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "217"
        artist = "James Paick"
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b5efb85-1e5f-40ba-97b1-0ef6ac680330.jpg?1783940652"
    }
}
