package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Slith Strider — Mirrodin #50
 * {1}{U}{U} · Creature — Slith · 1/1
 *
 * Whenever this creature becomes blocked, draw a card.
 * Whenever this creature deals combat damage to a player, put a +1/+1 counter on it.
 *
 * The blue member of the Slith cycle. Its two triggers pull in opposite directions: getting
 * blocked replaces the growth with a card, so it always trades up.
 *
 * Modelling notes:
 * - The unfiltered [Triggers.BecomesBlocked] is right here — the printed text has no blocker
 *   restriction, so a gang block still yields exactly one trigger and exactly one card
 *   (contrast Ogre Leadfoot in this set, which needs the per-blocker filtered form).
 * - [Triggers.DealsCombatDamageToPlayer] is *combat* damage only, matching the rest of the
 *   cycle: a burn spell or a damage-redirection effect never grows it. The counter goes on the
 *   Slith itself ([EffectTarget.Self]), not on a chosen creature.
 */
val SlithStrider = card("Slith Strider") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Slith"
    power = 1
    toughness = 1
    oracleText = "Whenever this creature becomes blocked, draw a card.\n" +
        "Whenever this creature deals combat damage to a player, put a +1/+1 counter on it."

    triggeredAbility {
        trigger = Triggers.BecomesBlocked
        effect = Effects.DrawCards(1)
        description = "Whenever this creature becomes blocked, draw a card."
    }

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever this creature deals combat damage to a player, put a +1/+1 counter on it."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "50"
        artist = "Justin Sweet"
        flavorText = "A slith's form and function are determined by the color of the sun under " +
            "which it's born."
        imageUri = "https://cards.scryfall.io/normal/front/8/5/85866351-a721-4b3c-9b6f-4d291daab657.jpg?1783944553"
    }
}
