package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Order of Midnight // Alter Fate
 * {1}{B}
 * Creature — Human Knight
 * 2/2
 * Flying
 * This creature can't block.
 *
 * Adventure: Alter Fate — {1}{B}, Sorcery — Adventure
 * Return target creature card from your graveyard to your hand.
 */
val OrderOfMidnight = card("Order of Midnight") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 2
    oracleText = "Flying\nThis creature can't block."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = CantBlock()
    }

    adventure("Alter Fate") {
        manaCost = "{1}{B}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Return target creature card from your graveyard to your hand. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            val t = target(TargetFilter.CreatureInYourGraveyard)
            effect = Effects.Move(t, Zone.HAND)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "99"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/330cc452-4382-401d-9432-ac27ae6e27ad.jpg?1783932635"
    }
}
