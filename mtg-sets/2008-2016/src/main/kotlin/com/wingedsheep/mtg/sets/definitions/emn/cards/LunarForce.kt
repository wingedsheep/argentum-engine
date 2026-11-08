package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect

/**
 * Lunar Force
 * {2}{U}
 * Enchantment
 * When an opponent casts a spell, sacrifice this enchantment and counter that spell.
 *
 * Monkey Cage's shape on Vexing Bauble's counter: the sacrifice is part of the effect, not a cost
 * or an "if you do". [SacrificeSelfEffect] is a no-op once Lunar Force has already left the
 * battlefield, and the non-targeted counter of the triggering spell still happens — so a second
 * trigger (a spell cast in response to the first) still counters. Likewise the sacrifice happens
 * even when the spell can't be countered or is already gone.
 */
val LunarForce = card("Lunar Force") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "When an opponent casts a spell, sacrifice this enchantment and counter that spell."

    triggeredAbility {
        trigger = Triggers.anOpponent.casts()
        effect = SacrificeSelfEffect then Effects.CounterTriggeringSpell()
        description = "When an opponent casts a spell, sacrifice Lunar Force and counter that spell."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "68"
        artist = "Eric Deschamps"
        flavorText = "\"Moonlight offers sublime warmth that brings me a comfort sunlight cannot provide.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76614db3-480f-41f6-91ad-66301fc8e394.jpg?1783937495"
        ruling("2016-07-13", "You can't choose not to sacrifice Lunar Force.")
        ruling(
            "2016-07-13",
            "You sacrifice Lunar Force even if the spell's already been countered, perhaps by the ability of " +
                "another Lunar Force, or if the spell can't be countered.",
        )
        ruling(
            "2016-07-13",
            "The spell will be countered even if Lunar Force can't be sacrificed, most likely because it has " +
                "already left the battlefield. Casting a spell in response to an opponent's Lunar Force's " +
                "triggered ability is generally a bad idea.",
        )
    }
}
