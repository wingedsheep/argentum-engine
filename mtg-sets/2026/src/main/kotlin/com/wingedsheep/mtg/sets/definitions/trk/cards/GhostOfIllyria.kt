package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ghost of Illyria
 * {3}{W}
 * Creature — Elemental Spirit
 * 3/3
 *
 * Flying
 * When this creature enters, exile up to one other target nonland permanent you control. Return
 * that card to the battlefield under its owner's control at the beginning of the next end step.
 *
 * The blink pattern from Elrond, Moon-Reader with a single optional target: "up to one" is
 * `optional = true`, "other" is [TargetFilter.OtherNonlandPermanent]'s `excludeSelf`, and
 * [Patterns.Exile.exileUntilEndStep] exiles it and schedules the delayed return.
 */
val GhostOfIllyria = card("Ghost of Illyria") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Elemental Spirit"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "When this creature enters, exile up to one other target nonland permanent you control. " +
        "Return that card to the battlefield under its owner's control at the beginning of the next end step."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(TargetFilter.OtherNonlandPermanent.youControl(), optional = true)
        effect = Patterns.Exile.exileUntilEndStep(permanent)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "20"
        artist = "Pace Wilder"
        flavorText = "The unique electromagnetic virus transformed Hetemit IX's colonists to living plasma."
        imageUri = "https://cards.scryfall.io/normal/front/9/9/99830058-c97a-4326-8c2f-8658cf8a1561.jpg?1793359367"
    }
}
