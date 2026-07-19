package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Training Regimen
 * {3}{G}
 * Enchantment — Uncommon (MSH #192)
 *
 * "Creatures you control with +1/+1 counters on them have trample."
 * "At the beginning of combat on your turn, put a +1/+1 counter on target creature you control."
 *
 * Implementation: the trample clause is a plain Layer 6 keyword grant over the counter-gated group
 * (same shape as Badgermole), so it tracks counters appearing and disappearing continuously. The
 * combat trigger targets, so the target is declared with `target(...)` and chosen as the ability
 * goes on the stack.
 */
val TrainingRegimen = card("Training Regimen") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Creatures you control with +1/+1 counters on them have trample.\n" +
        "At the beginning of combat on your turn, put a +1/+1 counter on target creature you control."

    staticAbility {
        ability = GrantKeyword(
            Keyword.TRAMPLE,
            GroupFilter(GameObjectFilter.Creature.youControl().withCounter(Counters.PLUS_ONE_PLUS_ONE)),
        )
    }

    triggeredAbility {
        trigger = Triggers.BeginCombat
        val creature = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, creature)
        description = "At the beginning of combat on your turn, put a +1/+1 counter on target creature you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "192"
        artist = "Marco Turini"
        flavorText = "\"Rerack your weights, heroes. Gym staff could actually die trying to do it.\"\n" +
            "—Avengers training room note"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a27aee6d-1fc3-4307-bb16-b964d723fe2f.jpg?1783902910"
    }
}
