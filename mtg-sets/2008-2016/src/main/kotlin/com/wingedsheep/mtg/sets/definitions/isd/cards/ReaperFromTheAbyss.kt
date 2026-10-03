package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Reaper from the Abyss
 * {3}{B}{B}{B}
 * Creature — Demon
 * 6/6
 *
 * Flying
 * Morbid — At the beginning of each end step, if a creature died this turn, destroy target
 * non-Demon creature.
 *
 * Morbid is an intervening-if (CR 603.4): with no creature dead this turn the ability never
 * triggers, so [Conditions.CreatureDiedThisTurn] sits on `interveningIf`. "Each end step" fires on
 * every player's turn — `Triggers.anyPlayer.beginningOf(Step.END)`, as on Old Flitterfang.
 */
val ReaperFromTheAbyss = card("Reaper from the Abyss") {
    manaCost = "{3}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    power = 6
    toughness = 6
    oracleText = "Flying\n" +
        "Morbid — At the beginning of each end step, if a creature died this turn, destroy target non-Demon creature."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        interveningIf = Conditions.CreatureDiedThisTurn
        val creature = target(TargetFilter.Creature.notSubtype(Subtype("Demon")))
        effect = Effects.Destroy(creature)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "112"
        artist = "Matt Stewart"
        flavorText = "\"Avacyn has deserted you. I welcome your devotion in her stead.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0d74c3e-8370-419b-808d-96b8d9306024.jpg"
    }
}
