package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pilgrim of the Ages
 * {2}{W}
 * Creature — Spirit
 * 2/1
 * When this creature enters, you may search your library for a basic Plains card, reveal it,
 * put it into your hand, then shuffle.
 * {6}: Return this card from your graveyard to your hand.
 */
val PilgrimOfTheAges = card("Pilgrim of the Ages") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Spirit"
    oracleText = "When this creature enters, you may search your library for a basic Plains card, " +
        "reveal it, put it into your hand, then shuffle.\n" +
        "{6}: Return this card from your graveyard to your hand."
    power = 2
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand.withSubtype(Subtype.PLAINS),
            destination = SearchDestination.HAND,
            reveal = true,
        )
    }

    activatedAbility {
        cost = Costs.Mana("{6}")
        effect = Effects.ReturnToHandFromGraveyard(EffectTarget.Self)
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "22"
        artist = "JiHun Lee"
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c86714f5-e909-413f-8eb6-99dbea4d1897.jpg?1783927388"
    }
}
