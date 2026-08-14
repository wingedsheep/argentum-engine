package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Goblin War Wagon — Mirrodin #179
 * {4} · Artifact Creature — Juggernaut · 3/3
 *
 * This creature doesn't untap during your untap step.
 * At the beginning of your upkeep, you may pay {2}. If you do, untap this creature.
 *
 * Modelling notes:
 * - The untap restriction is the narrow [AbilityFlag.DOESNT_UNTAP] ("doesn't untap during its
 *   controller's untap step"), *not* [AbilityFlag.CANT_BECOME_UNTAPPED] — an "untap target
 *   permanent" effect from elsewhere still untaps the Wagon, and so does the ability below.
 *   It is scoped to [GroupFilter.source] so the static only ever affects this permanent.
 * - The upkeep trigger is a plain "you may pay {2}. If you do, …" gate ([MayPayManaEffect]),
 *   which is the CR 601-style optional-cost payment window, not an additional cost or a
 *   cumulative upkeep. Declining simply leaves the Wagon tapped.
 */
val GoblinWarWagon = card("Goblin War Wagon") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Juggernaut"
    power = 3
    toughness = 3
    oracleText = "This creature doesn't untap during your untap step.\n" +
        "At the beginning of your upkeep, you may pay {2}. If you do, untap this creature."

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name, GroupFilter.source())
    }

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{2}"),
            effect = Effects.Untap(EffectTarget.Self)
        )
        description = "At the beginning of your upkeep, you may pay {2}. If you do, untap this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "179"
        artist = "Doug Chaffee"
        imageUri = "https://cards.scryfall.io/normal/front/2/2/229e8124-54ed-4c29-8c44-4962bd60f145.jpg?1783944519"
    }
}
