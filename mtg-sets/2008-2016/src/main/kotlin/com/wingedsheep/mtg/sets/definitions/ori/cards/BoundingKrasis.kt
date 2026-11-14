package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Bounding Krasis
 * {1}{G}{U}
 * Creature — Fish Lizard
 * 3/3
 * Flash
 * When this creature enters, you may tap or untap target creature.
 */
val BoundingKrasis = card("Bounding Krasis") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Fish Lizard"
    power = 3
    toughness = 3
    oracleText = "Flash (You may cast this spell any time you could cast an instant.)\n" +
        "When this creature enters, you may tap or untap target creature."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.Modal(
            modes = listOf(
                Mode.noTarget(Effects.Tap(creature)),
                Mode.noTarget(Effects.Untap(creature))
            ),
            chooseCount = 1,
            countsAsModalSpell = false
        )
        optional = true
        description = "When this creature enters, you may tap or untap target creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "212"
        artist = "Jack Wang"
        flavorText = "Unpredictable as a storm and destructive as a tidal wave."
        imageUri = "https://cards.scryfall.io/normal/front/6/6/660befe5-9c98-4aa3-9ca7-e643eaf91490.jpg?1783938315"
    }
}
