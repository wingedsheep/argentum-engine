package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter


/**
 * Ironclad Slayer
 * {2}{W}
 * Creature — Human Warrior
 * 3/2
 * When this creature enters, you may return target Aura or Equipment card from your graveyard to your hand.
 */
val IroncladSlayer = card("Ironclad Slayer") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Warrior"
    oracleText = "When this creature enters, you may return target Aura or Equipment card from your graveyard to your hand."
    power = 3
    toughness = 2
    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        val t = target(
            TargetFilter(
                GameObjectFilter.Any.withAnySubtype("Aura", "Equipment").ownedByYou(),
                zone = Zone.GRAVEYARD
            )
        )
        effect = Effects.Move(t, Zone.HAND)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "31"
        artist = "Ryan Pancoast"
        flavorText = "The best weapon is the one at hand."
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d413f69f-fe2d-4049-8640-178af7e927d9.jpg"
    }
}
