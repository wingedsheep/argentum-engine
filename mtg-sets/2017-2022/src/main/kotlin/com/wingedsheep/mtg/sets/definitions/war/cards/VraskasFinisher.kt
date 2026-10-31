package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Vraska's Finisher
 * {2}{B}
 * Creature — Gorgon Assassin
 * 3/2
 * When this creature enters, destroy target creature or planeswalker an opponent controls that was dealt damage this turn.
 */
val VraskasFinisher = card("Vraska's Finisher") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Gorgon Assassin"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, destroy target creature or planeswalker an opponent controls that was dealt damage this turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(
            TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.opponentControls().wasDealtDamageThisTurn())
        )
        effect = Effects.Destroy(permanent)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "112"
        artist = "Igor Kieryluk"
        flavorText = "\"You might have pleaded your case with my queen earlier. Now I am here, and it is far too late.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f89fa379-7fae-4fbf-8e31-0ef3645353c5.jpg?1783933435"
    }
}
