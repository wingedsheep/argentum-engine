package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Diligent Excavator
 * {1}{U}
 * Creature — Human Artificer
 * 1/3
 * Whenever you cast a historic spell, target player mills two cards.
 * (Artifacts, legendaries, and Sagas are historic.)
 */
val DiligentExcavator = card("Diligent Excavator") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Artificer"
    power = 1
    toughness = 3
    oracleText = "Whenever you cast a historic spell, target player mills two cards. (Artifacts, legendaries, and Sagas are historic.)"

    triggeredAbility {
        trigger = Triggers.YouCastHistoric
        val t = target("target", Targets.Player)
        effect = LibraryPatterns.mill(2, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "51"
        artist = "Mark Behm"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65212970-770e-4110-aa86-c89128688867.jpg?1562736818"
    }
}
