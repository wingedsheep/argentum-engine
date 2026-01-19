package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.AbilityCost
import com.wingedsheep.rulesengine.ability.CreateTokenEffect
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.TimingRestriction
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Clachan Festival
 *
 * {2}{W} Kindred Enchantment — Kithkin
 * When this enchantment enters, create two 1/1 green and white Kithkin creature tokens.
 * {4}{W}: Create a 1/1 green and white Kithkin creature token.
 */
object ClachanFestival {
    val definition = CardDefinition.kindredEnchantment(
        name = "Clachan Festival",
        manaCost = ManaCost.parse("{2}{W}"),
        subtypes = setOf(Subtype.KITHKIN),
        oracleText = "When this enchantment enters, create two 1/1 green and white Kithkin creature tokens.\n" +
                "{4}{W}: Create a 1/1 green and white Kithkin creature token.",
        metadata = ScryfallMetadata(
            collectorNumber = "10",
            rarity = Rarity.UNCOMMON,
            artist = "Kev Fang",
            flavorText = "Unmatched in their harmony, the troupe took home the festival's trophy with a stirring rendition of a traditional tune.",
            imageUri = "https://cards.scryfall.io/normal/front/3/2/324b5234-ffbf-4801-a475-8f693679ae2f.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Clachan Festival") {
        // ETB: Create two 1/1 green and white Kithkin creature tokens
        triggered(
            trigger = OnEnterBattlefield(),
            effect = CreateTokenEffect(
                count = 2,
                power = 1,
                toughness = 1,
                colors = setOf(Color.GREEN, Color.WHITE),
                creatureTypes = setOf("Kithkin")
            )
        )

        // Activated ability: {4}{W}: Create a 1/1 green and white Kithkin creature token
        activated(
            cost = AbilityCost.Mana(white = 1, generic = 4),
            effect = CreateTokenEffect(
                count = 1,
                power = 1,
                toughness = 1,
                colors = setOf(Color.GREEN, Color.WHITE),
                creatureTypes = setOf("Kithkin")
            ),
            timing = TimingRestriction.INSTANT
        )
    }
}
