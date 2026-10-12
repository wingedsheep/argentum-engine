package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Boulderbranch Golem
 * {7}
 * Artifact Creature — Golem
 * 6/5
 * Prototype {3}{G} — 3/3
 * When this creature enters, you gain life equal to its power.
 *
 * `sourcePower()` reads the permanent's live (or last-known) power, so a prototyped Golem gains 3.
 */
val BoulderbranchGolem = card("Boulderbranch Golem") {
    manaCost = "{7}"
    colorIdentity = "G"
    typeLine = "Artifact Creature — Golem"
    power = 6
    toughness = 5
    oracleText = "Prototype {3}{G} — 3/3 (You may cast this spell with different mana cost, color, and size. It keeps its abilities and types.)\n" +
        "When this creature enters, you gain life equal to its power."

    keywordAbility(KeywordAbility.prototype("{3}{G}", 3, 3))

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(DynamicAmounts.sourcePower())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "197"
        artist = "Dan Murayama Scott"
        flavorText = "\"Artifice is not the only form of creation.\"\n—Titania"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6e6e5338-9b95-4ac7-a57c-5efaa6423531.jpg?1783920037"
        ruling("2022-10-14", "If Boulderbranch Golem leaves the battlefield before its enters-the-battlefield triggered ability resolves, use its power as it last existed on the battlefield to determine how much life is gained, not its power in the zone it moved to.")
    }
}
