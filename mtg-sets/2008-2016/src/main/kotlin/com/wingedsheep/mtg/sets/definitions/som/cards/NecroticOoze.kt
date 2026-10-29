package com.wingedsheep.mtg.sets.definitions.som.cards

import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DonorCards
import com.wingedsheep.sdk.scripting.HasAllActivatedAbilitiesOfCards

/**
 * Necrotic Ooze
 * {2}{B}{B}
 * Creature — Ooze
 * 4/3
 *
 * As long as this creature is on the battlefield, it has all activated abilities of all creature
 * cards in all graveyards.
 *
 * The Mirran Safehouse shape: the `ALL_GRAVEYARDS` arm of [HasAllActivatedAbilitiesOfCards],
 * narrowed to creature cards and granted to itself. The donor pool is re-read live, and granted
 * abilities name the Ooze in place of the donor card — both per the rulings below.
 */
val NecroticOoze = card("Necrotic Ooze") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Ooze"
    power = 4
    toughness = 3
    oracleText = "As long as this creature is on the battlefield, it has all activated abilities of " +
        "all creature cards in all graveyards."

    staticAbility {
        ability = HasAllActivatedAbilitiesOfCards(
            donors = DonorCards.ALL_GRAVEYARDS,
            cardFilter = Filters.Creature
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "72"
        artist = "James Ryman"
        flavorText = "\"To death all must go, and so in death lies ultimate power.\"\n—Geth, Lord of the Vault"
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8af2c79f-a151-4628-90fe-c0ff7ccd9c2c.jpg?1783941730"
        ruling("2011-01-01", "Necrotic Ooze gains only activated abilities. It doesn't gain keyword abilities (unless those keyword abilities are activated), triggered abilities, or static abilities.")
        ruling("2011-01-01", "If an activated ability of a card in a graveyard references the card it's printed on by name, treat Necrotic Ooze's version of that ability as though it referenced Necrotic Ooze by name instead. For example, if Cudgel Troll (which says \"{G}: Regenerate Cudgel Troll\") is in a graveyard, Necrotic Ooze has the ability \"{G}: Regenerate Necrotic Ooze.\"")
    }
}
