package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DonorCards
import com.wingedsheep.sdk.scripting.HasAllActivatedAbilitiesOfCards

/**
 * Mirran Safehouse
 * {3}
 * Artifact (Phyrexia: All Will Be One, rare)
 *
 * "As long as this artifact is on the battlefield, it has all activated abilities of all land
 *  cards in all graveyards."
 *
 * The `ALL_GRAVEYARDS` arm of [HasAllActivatedAbilitiesOfCards], narrowed to land cards and
 * granted to itself. Land donors with basic land types also lend their intrinsic mana abilities
 * (CR 305.6), per the ruling below — the engine derives them from the donor's type line.
 */
val MirranSafehouse = card("Mirran Safehouse") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "As long as this artifact is on the battlefield, it has all activated abilities of " +
        "all land cards in all graveyards."

    staticAbility {
        ability = HasAllActivatedAbilitiesOfCards(
            donors = DonorCards.ALL_GRAVEYARDS,
            cardFilter = Filters.Land
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "232"
        artist = "Piotr Dura"
        flavorText = "\"Melira helped me design it. Those who are infected by the oil cannot cross the " +
            "threshold. We should be safe inside.\"\n—Koth"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d76fe2f2-ce2c-49e4-9f00-89c82f2fae8d.jpg?1783917988"
        ruling("2023-02-04", "Mirran Safehouse gains only activated abilities, including activated mana abilites. It doesn't gain keyword abilities (unless those keyword abilities are activated), triggered abilities, or static abilities.")
        ruling("2023-02-04", "Cards with basic land types intrinsically have the appropriate mana ability, even if it isn't printed in the card's text box. Mirran Safehouse also gains those abilities. For example, if there is a Plains in your graveyard, Mirran Safehouse has \"{T}: Add {W},\" even though that may not be printed on the Plains itself.")
        ruling("2023-02-04", "If an activated ability of a land card in a graveyard references the card it's printed on by name, treat Mirran Safehouse's version of that ability as though it referenced Mirran Safehouse by name instead.")
    }
}
