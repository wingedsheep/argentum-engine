package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.becomesDayAsEntersIfNeither
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Component Collector
 * {2}{U}
 * Creature — Homunculus
 * 1/4
 * If it's neither day nor night, it becomes day as this creature enters.
 * Whenever day becomes night or night becomes day, you may tap or untap target nonland permanent.
 *
 * The tap-or-untap choice is made on resolution — the [com.wingedsheep.mtg.sets.definitions.lrw.cards.Pestermite]
 * idiom: a [Effects.May] over a two-[Mode] modal with `countsAsModalSpell = false`.
 */
val ComponentCollector = card("Component Collector") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Homunculus"
    power = 1
    toughness = 4
    oracleText = "If it's neither day nor night, it becomes day as this creature enters.\n" +
        "Whenever day becomes night or night becomes day, you may tap or untap target nonland permanent."

    becomesDayAsEntersIfNeither()

    triggeredAbility {
        trigger = Triggers.dayBecomesNightOrNightBecomesDay
        val permanent = target(TargetFilter.NonlandPermanent)
        effect = Effects.May(
            Effects.Modal(
                modes = listOf(
                    Mode.noTarget(Effects.Tap(permanent), "Tap that permanent"),
                    Mode.noTarget(Effects.Untap(permanent), "Untap that permanent")
                ),
                chooseCount = 1,
                countsAsModalSpell = false
            )
        )
        description = "Whenever day becomes night or night becomes day, you may tap or untap target nonland permanent."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "43"
        artist = "Mark Behm"
        flavorText = "\"Keep master busy with new bits or else maybe she wonders if I'm made of bits too.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/5/956b74fc-1355-4668-838b-e8927794608c.jpg?1783925648"
    }
}
