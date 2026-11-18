package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Skybind
 * {3}{W}{W}
 * Enchantment
 *
 * Constellation — Whenever this enchantment or another enchantment you control enters, exile target
 * nonenchantment permanent. Return that card to the battlefield under its owner's control at the
 * beginning of the next end step.
 *
 * Skybind is itself an enchantment, so one unbound watcher over enchantments you control covers its
 * own entry too (Harvestguard Alseids' shape). The exile-and-return is the plain blink pattern,
 * [Patterns.Exile.exileUntilEndStep], aimed at a `Permanent and Nonenchantment` target.
 */
val Skybind = card("Skybind") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Constellation — Whenever this enchantment or another enchantment you control enters, exile " +
        "target nonenchantment permanent. Return that card to the battlefield under its owner's control at " +
        "the beginning of the next end step."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Enchantment.youControl()).enters()
        val permanent = target(TargetFilter(GameObjectFilter.Permanent and GameObjectFilter.Nonenchantment))
        effect = Patterns.Exile.exileUntilEndStep(permanent)
        description = "Constellation — Whenever this enchantment or another enchantment you control enters, " +
            "exile target nonenchantment permanent. Return that card to the battlefield under its owner's " +
            "control at the beginning of the next end step."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "25"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/91e3117a-0955-47fc-81c0-7a662c8c487f.jpg?1783939455"
        ruling(
            "2014-04-26",
            "A constellation ability triggers whenever an enchantment enters the battlefield under your " +
                "control for any reason. Enchantments with other card types, such as enchantment creatures, " +
                "will also cause constellation abilities to trigger."
        )
    }
}
