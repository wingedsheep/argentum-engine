package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Emergency Weld
 * {1}{B}
 * Sorcery
 * Return target artifact or creature card from your graveyard to your hand. Create a 1/1 colorless
 * Soldier artifact creature token.
 */
val EmergencyWeld = card("Emergency Weld") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Return target artifact or creature card from your graveyard to your hand. " +
        "Create a 1/1 colorless Soldier artifact creature token."

    spell {
        val t = target(
            TargetFilter((GameObjectFilter.Artifact or GameObjectFilter.Creature).ownedByYou(), zone = Zone.GRAVEYARD)
        )
        effect = Effects.Move(target = t, destination = Zone.HAND) then Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Soldier"),
            artifactToken = true
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "93"
        artist = "Raoul Vitale"
        flavorText = "Scurrying repair bots quietly turned the tide of many battles, patching damaged automatons back into fighting form."
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ec94d440-3922-4588-bf7b-d8670f81ef4e.jpg"
    }
}
