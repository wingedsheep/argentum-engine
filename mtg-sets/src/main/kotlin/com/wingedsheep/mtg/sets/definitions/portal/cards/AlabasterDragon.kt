package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.MoveToZoneEffect
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.ZonePlacement

/**
 * Alabaster Dragon
 * {4}{W}{W}
 * Creature — Dragon
 * 4/4
 * Flying
 * When Alabaster Dragon dies, shuffle it into its owner's library.
 */
val AlabasterDragon = card("Alabaster Dragon") {
    manaCost = "{4}{W}{W}"
    typeLine = "Creature — Dragon"
    power = 4
    toughness = 4
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.Dies
        effect = MoveToZoneEffect(EffectTarget.Self, Zone.LIBRARY, ZonePlacement.Shuffled)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "1"
        artist = "Ted Naifeh"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1edc6ec1-3b34-45e0-8573-39eba1d10efa.jpg"
    }
}
