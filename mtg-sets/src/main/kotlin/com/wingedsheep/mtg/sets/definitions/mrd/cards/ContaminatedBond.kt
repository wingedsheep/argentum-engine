package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/** "its controller loses 3 life" — the granted ability is controlled by the enchanted creature's
 *  controller, so [EffectTarget.Controller] is exactly "its controller". */
private val controllerLosesThree = Effects.LoseLife(3, EffectTarget.Controller)

/**
 * Contaminated Bond — Mirrodin #61
 * {1}{B} · Enchantment — Aura
 *
 * Enchant creature
 * Whenever enchanted creature attacks or blocks, its controller loses 3 life.
 *
 * A punisher Aura: it goes on a creature you *don't* control and taxes its owner for using it.
 * That makes the controller distinction load-bearing rather than incidental — the life loss must
 * hit the creature's controller, never the Aura's.
 *
 * One printed ability with two trigger conditions, so it is the Super-Soldier Serum idiom: two
 * [GrantTriggeredAbility] statics over [Filters.EnchantedCreature], one for [Triggers.Attacks]
 * and one for [Triggers.Blocks]. Installing the triggers *on the creature* rather than keeping
 * them on the Aura is required, not stylistic — the engine's `AttachmentTriggerDetector` has no
 * block branch, so an `ATTACHED`-bound blocks trigger would never fire. It also lands the
 * controller question on the right answer for free: the granted ability's controller is the
 * creature's controller, which is what [EffectTarget.Controller] reads.
 *
 * "Attacks or blocks" is two separate triggers, and a creature that attacks this turn and blocks
 * on the crawl-back does lose its controller 3 life each time. A creature that blocks two
 * attackers at once still only triggers the blocks half once (it "blocks" a single time).
 */
val ContaminatedBond = card("Contaminated Bond") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Whenever enchanted creature attacks or blocks, its controller loses 3 life."

    auraTarget = Targets.Creature

    // "Whenever enchanted creature attacks ..."
    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.Attacks.event,
                binding = Triggers.Attacks.binding,
                effect = controllerLosesThree,
                descriptionOverride = "Whenever this creature attacks, its controller loses 3 life.",
            ),
            filter = Filters.EnchantedCreature,
        )
    }

    // "... or blocks, its controller loses 3 life."
    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.Blocks.event,
                binding = Triggers.Blocks.binding,
                effect = controllerLosesThree,
                descriptionOverride = "Whenever this creature blocks, its controller loses 3 life.",
            ),
            filter = Filters.EnchantedCreature,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "61"
        artist = "Thomas M. Baxa"
        flavorText = "This leash disciplines the master."
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a1a64356-3064-47a2-80be-5e2f56c85556.jpg?1783944549"
    }
}
