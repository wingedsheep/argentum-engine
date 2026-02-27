package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CantBeRegeneratedEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Skinthinner
 * {1}{B}
 * Creature — Zombie
 * 2/1
 * Morph {3}{B}{B}
 * When this creature is turned face up, destroy target nonblack creature. It can't be regenerated.
 */
val Skinthinner = card("Skinthinner") {
    manaCost = "{1}{B}"
    typeLine = "Creature — Zombie"
    power = 2
    toughness = 1
    oracleText = "Morph {3}{B}{B} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen this creature is turned face up, destroy target nonblack creature. It can't be regenerated."

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        val t = target("nonblack creature", TargetCreature(filter = TargetFilter.Creature.notColor(Color.BLACK)))
        effect = CantBeRegeneratedEffect(t) then
                MoveToZoneEffect(t, Zone.GRAVEYARD, byDestruction = true)
    }

    morph = "{3}{B}{B}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "80"
        artist = "Dany Orizio"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89b8c392-da68-4894-b6e8-eb430141a0d7.jpg?1562922833"
    }
}
