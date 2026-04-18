package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Bonebind Orator {1}{B}
 * Creature — Squirrel Warlock Bard
 * 2/2
 *
 * {3}{B}, Exile this card from your graveyard: Return another target creature card
 * from your graveyard to your hand.
 */
val BonebindOrator = card("Bonebind Orator") {
    manaCost = "{1}{B}"
    typeLine = "Creature — Squirrel Warlock Bard"
    power = 2
    toughness = 2
    oracleText = "{3}{B}, Exile this card from your graveyard: Return another target creature card from your graveyard to your hand."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{B}"), Costs.ExileSelf)
        activateFromZone = Zone.GRAVEYARD
        val creature = target(
            "another target creature card from your graveyard",
            TargetObject(filter = TargetFilter.CreatureInYourGraveyard.other())
        )
        effect = Effects.ReturnToHand(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "84"
        artist = "Tuan Duong Chu"
        flavorText = "A great actor can breathe new life into old material."
        imageUri = "https://cards.scryfall.io/normal/front/f/a/faf226fa-ca09-4468-8804-87b2a7de2c66.jpg?1721426343"
    }
}
