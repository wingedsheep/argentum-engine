package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Renowned Weaver
 * {G}
 * Creature — Human Shaman
 * 1/1
 *
 * {1}{G}, Sacrifice this creature: Create a 1/3 green Spider enchantment creature token with reach.
 *
 * The token is an *enchantment* creature (`enchantmentToken = true`), so it feeds constellation.
 * Its art comes from Journey into Nyx's own token sheet (the 1/3 green Spider), synced into
 * `tokens.json`.
 */
val RenownedWeaver = card("Renowned Weaver") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Shaman"
    power = 1
    toughness = 1
    oracleText = "{1}{G}, Sacrifice this creature: Create a 1/3 green Spider enchantment creature token " +
        "with reach. (It can block creatures with flying.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{G}"), Costs.SacrificeSelf)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 3,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Spider"),
            keywords = setOf(Keyword.REACH),
            enchantmentToken = true,
        )
        description = "{1}{G}, Sacrifice this creature: Create a 1/3 green Spider enchantment creature " +
            "token with reach."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "137"
        artist = "Yohann Schepacz"
        flavorText = "\"You think your weaving as beautiful as mine? Let me show you the true meaning of the craft.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/4/240830be-53f8-4f46-a0fb-6ecc439d1349.jpg?1783939409"
    }
}
