package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Scourge of the Undercity — Foundations Jumpstart #11
 * {1}{B} · Creature — Gorgon Vampire · 2/1
 *
 * Lifelink
 * When this creature enters, another target creature you control gains lifelink until end of turn.
 */
val ScourgeOfTheUndercity = card("Scourge of the Undercity") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Gorgon Vampire"
    power = 2
    toughness = 1
    oracleText = "Lifelink (Damage dealt by this creature also causes you to gain that much life.)\n" +
        "When this creature enters, another target creature you control gains lifelink until end of turn."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.OtherCreatureYouControl)
        effect = Effects.GrantKeyword(Keyword.LIFELINK, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "11"
        artist = "Olivier Bernard"
        flavorText = "Her hunger for blood is matched only by her hunger for vengeance."
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d321cb1-9079-4aa3-b86e-6483552f04ee.jpg?1783908867"
    }
}
