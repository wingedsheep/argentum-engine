package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCantBeCountered
import com.wingedsheep.sdk.scripting.GraveyardCardsHaveFlashback

/**
 * Lier, Disciple of the Drowned
 * {3}{U}{U}
 * Legendary Creature — Human Wizard
 * 3/4
 * Spells can't be countered.
 * Each instant and sorcery card in your graveyard has flashback. The flashback cost is equal to
 *   that card's mana cost.
 *
 * "Spells can't be countered" is a global [GrantCantBeCountered] over every spell, whoever controls
 * it. The flashback clause is a whole-graveyard [GraveyardCardsHaveFlashback] grant; a null cost
 * means "equal to that card's mana cost" (current Oracle has no your-turn restriction).
 */
val LierDiscipleOfTheDrowned = card("Lier, Disciple of the Drowned") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Wizard"
    power = 3
    toughness = 4
    oracleText = "Spells can't be countered.\n" +
        "Each instant and sorcery card in your graveyard has flashback. " +
        "The flashback cost is equal to that card's mana cost."

    staticAbility {
        ability = GrantCantBeCountered(filter = GameObjectFilter.Any)
    }

    staticAbility {
        ability = GraveyardCardsHaveFlashback(filter = GameObjectFilter.InstantOrSorcery)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "59"
        artist = "Ekaterina Burmak"
        flavorText = "Beneath the waves, lost gods stir in their slumber."
        imageUri = "https://cards.scryfall.io/normal/front/7/8/78fb8900-d28d-4e33-96a7-66fcbc117adf.jpg?1783925638"
        ruling("2025-06-06", "If a card with no mana cost gains flashback, it has no flashback cost. It can't be cast this way.")
        ruling("2025-06-06", "If you cast an instant or sorcery with {X} in its mana cost this way, you still choose the value of X as part of casting the spell and pay that cost.")
        ruling("2025-06-06", "You must still follow any timing restrictions and permissions, including those based on the card's type. For instance, you can cast a sorcery using flashback only when you could normally cast a sorcery.")
        ruling("2025-06-06", "A spell cast using flashback will always be exiled afterward, whether it resolves, is countered, or leaves the stack in some other way.")
    }
}
