package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.TargetSpellOrPermanent

/**
 * Chaoslace
 * {R}
 * Instant
 *
 * Target spell or permanent becomes red. (Its mana symbols remain unchanged.)
 */
val Chaoslace = card("Chaoslace") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target spell or permanent becomes red. (Its mana symbols remain unchanged.)"

    spell {
        val subject = target(TargetSpellOrPermanent())
        effect = Effects.ChangeColor(subject, setOf(Color.RED), Duration.Permanent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "139"
        artist = "Dameon Willich"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/72ea2048-57bc-43d5-8987-33ca727f1a97.jpg?1783948689"
    }
}
