package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Parasitic Grasp
 * {1}{B}
 * Instant
 * Cleave {1}{B}{B} (You may cast this spell for its cleave cost. If you do, remove the words in
 * square brackets.)
 * Parasitic Grasp deals 3 damage to target [Human] creature. You gain 3 life.
 *
 * Cleave (CR 702.148) removes the bracketed words when its alternative cost is paid. The printed
 * (cheaper) cast can only hit a Human creature (drain-a-villager flavor); paying the cleave cost
 * broadens the target to any creature.
 *
 * Target-only difference: the base [target] carries the "Human" subtype restriction and
 * [cleaveTarget] drops it. Both modes deal 3 damage to the chosen target and gain 3 life, so only
 * the target requirement is swapped — the 3-damage-plus-gain-3 effect is shared. The life gain is
 * unconditional (not tied to damage dealt), so it always resolves.
 */
val ParasiticGrasp = card("Parasitic Grasp") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Cleave {1}{B}{B} (You may cast this spell for its cleave cost. If you do, " +
        "remove the words in square brackets.)\nParasitic Grasp deals 3 damage to target [Human] " +
        "creature. You gain 3 life."

    keywordAbility(KeywordAbility.cleave("{1}{B}{B}"))

    spell {
        // Printed (brackets present): 3 damage to target Human creature, gain 3 life.
        val human = target(
            "Human creature",
            TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.withSubtype(Subtype.HUMAN))),
        )
        effect = Effects.DealDamage(3, human).then(Effects.GainLife(3))

        // Cleaved (brackets removed): 3 damage to target creature, gain 3 life.
        val anyCreature = cleaveTarget("creature", Targets.Creature)
        cleaveEffect = Effects.DealDamage(3, anyCreature).then(Effects.GainLife(3))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "123"
        artist = "Rovina Cai"
        flavorText = "Some wicked souls linger only to consume the warmth of life."
        imageUri = "https://cards.scryfall.io/normal/front/0/7/0713adf7-1770-4d5b-80f7-d6cbd24f7890.jpg?1783924860"
    }
}
