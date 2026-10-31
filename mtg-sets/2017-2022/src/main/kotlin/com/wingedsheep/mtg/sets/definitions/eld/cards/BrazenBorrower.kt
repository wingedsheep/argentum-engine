package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWith
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Brazen Borrower // Petty Theft
 * {1}{U}{U}
 * Creature — Faerie Rogue
 * 3/1
 *
 * Flash
 * Flying
 * This creature can block only creatures with flying.
 *
 * Adventure: Petty Theft — {1}{U}, Instant — Adventure
 * Return target nonland permanent an opponent controls to its owner's hand.
 */
val BrazenBorrower = card("Brazen Borrower") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Faerie Rogue"
    power = 3
    toughness = 1
    oracleText = "Flash\nFlying\nThis creature can block only creatures with flying."

    keywords(Keyword.FLASH, Keyword.FLYING)

    staticAbility {
        ability = CanOnlyBlockCreaturesWith(blockerFilter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING))
    }

    adventure("Petty Theft") {
        manaCost = "{1}{U}"
        typeLine = "Instant — Adventure"
        oracleText = "Return target nonland permanent an opponent controls to its owner's hand. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            val permanent = target(TargetFilter.NonlandPermanentOpponentControls)
            effect = Effects.ReturnToHand(permanent)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "39"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2089ec9-0665-448f-bfe9-d181de127814.jpg?1783932663"
    }
}
