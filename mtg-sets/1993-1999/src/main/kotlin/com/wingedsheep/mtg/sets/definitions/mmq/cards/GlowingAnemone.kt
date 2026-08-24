package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Glowing Anemone
 * {3}{U}
 * Creature — Jellyfish Beast
 * 1 / 3
 *
 * The optional-ETB bounce frame: [Triggers.EntersBattlefield] with `optional = true` (the printed
 * "you may") over a plain move-to-hand of the targeted land. "Its owner's hand" is the default
 * destination owner, so no controller override is needed.
 */
val GlowingAnemone = card("Glowing Anemone") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Jellyfish Beast"
    oracleText = "When this creature enters, you may return target land to its owner's hand."
    power = 1
    toughness = 3

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        optional = true
        val t = target("target", TargetPermanent(filter = TargetFilter.Land))
        effect = Effects.Move(t, Zone.HAND)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "81"
        artist = "Pete Venters"
        flavorText = "Beautiful to behold, terrible to be held."
        imageUri = "https://cards.scryfall.io/normal/front/7/0/708593e6-787b-4f76-a86c-1d52857493ea.jpg"
    }
}
