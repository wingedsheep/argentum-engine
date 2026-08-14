package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.events.AttackPredicate

/**
 * HYDRA Infiltration — Marvel Super Heroes #100
 * {3}{B} · Enchantment
 *
 * When this enchantment enters, target opponent discards two cards.
 * Whenever a creature you control attacks alone, target opponent loses 1 life and you gain 1 life.
 *
 * Implementation notes:
 * - "Attacks alone" is the Grasping Shadows shape: [Triggers.attacks] over "creature you control"
 *   with [AttackPredicate.Alone] and a [TriggerBinding.ANY] binding, since the enchantment itself
 *   is never the attacker.
 * - The drain is two independent effects, not [Effects.DrainLife]: the oracle text gains a flat 1
 *   life rather than "life equal to the life lost this way", so the gain still happens if the
 *   opponent's life loss is replaced or prevented.
 */
val HydraInfiltration = card("HYDRA Infiltration") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, target opponent discards two cards.\n" +
        "Whenever a creature you control attacks alone, target opponent loses 1 life and you " +
        "gain 1 life."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val victim = target("target opponent", Targets.Opponent)
        effect = Effects.Discard(2, victim)
        description = "When this enchantment enters, target opponent discards two cards."
    }

    triggeredAbility {
        trigger = Triggers.attacks(
            filter = GameObjectFilter.Creature.youControl(),
            requires = setOf(AttackPredicate.Alone),
            binding = TriggerBinding.ANY,
        )
        val victim = target("target opponent", Targets.Opponent)
        effect = Effects.Composite(
            Effects.LoseLife(1, victim),
            Effects.GainLife(1),
        )
        description = "Whenever a creature you control attacks alone, target opponent loses 1 " +
            "life and you gain 1 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "100"
        artist = "Eli Minaya"
        flavorText = "\"HYDRA has spread its tentacles throughout all levels of society. The " +
            "only question is, where will we strike next?\""
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2e446e1-e384-4d5f-8099-544f78c08510.jpg?1783902941"
    }
}
