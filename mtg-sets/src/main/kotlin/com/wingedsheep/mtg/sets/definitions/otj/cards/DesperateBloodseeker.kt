package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Desperate Bloodseeker
 * {1}{B}
 * Creature — Vampire
 * 2/2
 * Lifelink
 * When this creature enters, target player mills two cards.
 */
val DesperateBloodseeker = card("Desperate Bloodseeker") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire"
    power = 2
    toughness = 2
    oracleText = "Lifelink\nWhen this creature enters, target player mills two cards."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val player = target("target player", Targets.Player)
        effect = Patterns.Library.mill(2, player)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "86"
        artist = "Camille Alquier"
        flavorText = "With no mammals around, some vampires tried to feed on cactusfolk, " +
            "with horrible results for everyone involved."
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a59da027-b5dd-4920-b3a1-9da05fcb1977.jpg?1712355583"
    }
}
