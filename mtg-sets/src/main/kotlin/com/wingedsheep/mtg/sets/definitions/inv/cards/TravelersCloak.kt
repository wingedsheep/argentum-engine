package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GrantLandwalkOfChosenType

/**
 * Traveler's Cloak
 * {2}{U}
 * Enchantment — Aura
 * Enchant creature
 * As this Aura enters, choose a land type.
 * When this Aura enters, draw a card.
 * Enchanted creature has landwalk of the chosen type. (It can't be blocked as long as defending
 * player controls a land of that type.)
 *
 * Scoping note: the engine models landwalk only for the five basic land types (the only land
 * subtypes with a corresponding landwalk keyword), so the entry choice is restricted to
 * `BASIC_LAND_TYPE`. [GrantLandwalkOfChosenType] reads the recorded `ChosenLandTypeComponent` at
 * projection time and grants the matching keyword (Plains→Plainswalk, …).
 */
val TravelersCloak = card("Traveler's Cloak") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "As this Aura enters, choose a land type.\n" +
        "When this Aura enters, draw a card.\n" +
        "Enchanted creature has landwalk of the chosen type. (It can't be blocked as long as " +
        "defending player controls a land of that type.)"

    auraTarget = Targets.Creature

    replacementEffect(EntersWithChoice(ChoiceType.BASIC_LAND_TYPE))

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.DrawCards(1)
    }

    staticAbility {
        ability = GrantLandwalkOfChosenType()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "83"
        artist = "Rebecca Guay"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/977f0f82-0542-40c9-9a48-73077941dbd1.jpg?1562925547"
    }
}
