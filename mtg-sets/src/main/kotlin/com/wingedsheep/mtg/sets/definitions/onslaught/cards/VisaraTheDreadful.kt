package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Visara the Dreadful
 * {3}{B}{B}{B}
 * Legendary Creature — Gorgon
 * 5/5
 * Flying
 * {T}: Destroy target creature. It can't be regenerated.
 */
val VisaraTheDreadful = card("Visara the Dreadful") {
    manaCost = "{3}{B}{B}{B}"
    typeLine = "Legendary Creature — Gorgon"
    power = 5
    toughness = 5
    oracleText = "Flying\n{T}: Destroy target creature. It can't be regenerated."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = AbilityCost.Tap
        val t = target("target", TargetCreature())
        effect = MoveToZoneEffect(t, Zone.GRAVEYARD, byDestruction = true)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "179"
        artist = "Kev Walker"
        flavorText = "\"My eyes are my strongest feature.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce6adcfe-b0f7-4a96-bab2-f76c84ef5ca6.jpg?1562943731"
    }
}
