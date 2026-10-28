package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cleric of Chill Depths — Zendikar Rising #51
 * {1}{U} · Creature — Merfolk Cleric · 1/3
 *
 * Whenever this creature blocks a creature, that creature doesn't untap during its controller's
 * next untap step.
 *
 * One trigger per blocked creature (Wall of Dust's shape), granting `DOESNT_UNTAP` to that attacker
 * through its controller's next untap step (Barl's Cage's duration). The grant is a floating effect,
 * so it persists if the Cleric leaves the battlefield, and it never taps the creature.
 */
val ClericOfChillDepths = card("Cleric of Chill Depths") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Cleric"
    power = 1
    toughness = 3
    oracleText = "Whenever this creature blocks a creature, that creature doesn't untap during its " +
        "controller's next untap step."

    triggeredAbility {
        trigger = Triggers.self.blocks(attackerFilter = GameObjectFilter.Creature)
        effect = Effects.GrantKeyword(
            AbilityFlag.DOESNT_UNTAP,
            EffectTarget.TriggeringEntity,
            Duration.UntilAfterAffectedControllersNextUntap(),
        )
        description = "Whenever this creature blocks a creature, that creature doesn't untap during its " +
            "controller's next untap step."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "51"
        artist = "Livia Prima"
        flavorText = "\"Before my people turned to false gods, we built a glittering city beneath the " +
            "northern sea ice. The magic of ancient Benthidrix lives in me!\""
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b4ea262c-ea32-4aca-b96b-58f556a8dffc.jpg?1783929400"
        ruling(
            "2020-09-25",
            "The blocked creature doesn't untap even if Cleric of Chill Depths leaves the battlefield " +
                "before that creature's controller's next upkeep.",
        )
        ruling(
            "2020-09-25",
            "If the blocked creature is untapped (most likely because it has vigilance), the ability " +
                "doesn't tap it.",
        )
    }
}
