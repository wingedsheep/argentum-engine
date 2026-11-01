package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cavalcade of Calamity
 * {1}{R}
 * Enchantment
 * Whenever a creature you control with power 1 or less attacks, this enchantment deals 1 damage to
 * the player or planeswalker that creature is attacking.
 *
 * One trigger per qualifying attacker; power is checked only as it triggers. The damage goes to
 * that creature's declared defender ([EffectTarget.AttackedBy]) — read from its last-known
 * information if it has left the battlefield, and nothing at all when it attacks a battle.
 */
val CavalcadeOfCalamity = card("Cavalcade of Calamity") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature you control with power 1 or less attacks, this enchantment deals 1 damage " +
        "to the player or planeswalker that creature is attacking."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.powerAtMost(1).youControl()).attacks()
        effect = Effects.DealDamage(1, EffectTarget.AttackedBy())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "95"
        artist = "Jonas De Ro"
        flavorText = "Let the Dimir worry about witnesses. For the Rakdos, screams count as applause."
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8a81e889-490b-4aeb-8e84-ea9a390bb8fe.jpg?1783933685"
        ruling(
            "2019-01-25",
            "The power of the attacking creature is checked only when the ability triggers. Once it triggers, " +
                "Cavalcade of Calamity will deal 1 damage to the appropriate player or planeswalker even if the " +
                "creature's power changes or the creature leaves the battlefield before the ability resolves."
        )
    }
}
