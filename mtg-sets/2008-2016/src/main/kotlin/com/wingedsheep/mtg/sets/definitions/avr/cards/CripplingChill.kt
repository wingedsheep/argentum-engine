package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Crippling Chill
 * {2}{U}
 * Instant
 * Tap target creature. It doesn't untap during its controller's next untap step.
 * Draw a card.
 */
val CripplingChill = card("Crippling Chill") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Tap target creature. It doesn't untap during its controller's next untap step.\nDraw a card."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Tap(creature) then
            Effects.GrantKeyword(AbilityFlag.DOESNT_UNTAP, creature, Duration.UntilAfterAffectedControllersNextUntap()) then
            Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "46"
        artist = "Svetlin Velinov"
        flavorText = "One breath of the geist turns veins to rivers of ice and freezes hearts midbeat."
        imageUri = "https://cards.scryfall.io/normal/front/7/9/79791bd9-aded-48d9-866d-9f7bd6848905.jpg?1592708482"
    }
}
