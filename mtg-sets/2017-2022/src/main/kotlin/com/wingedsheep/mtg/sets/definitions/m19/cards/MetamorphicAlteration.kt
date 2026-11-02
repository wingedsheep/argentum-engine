package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Metamorphic Alteration — Core Set 2019 #60 (canonical printing)
 * {1}{U} Enchantment — Aura
 *
 * Enchant creature
 * As this Aura enters, choose a creature.
 * Enchanted creature is a copy of the chosen creature.
 *
 * The creature is chosen as the Aura enters ([EntersWithChoice]), and the copy is the rest of that
 * choice: the chosen creature's copiable values are locked in then, so it later leaving or changing
 * doesn't touch the copy. [Duration.WhileSourceAttached] makes the copy the Aura's own static —
 * it follows the Aura to whatever creature it enchants and ends when the Aura leaves.
 */
val MetamorphicAlteration = card("Metamorphic Alteration") {
    manaCost = "{1}{U}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "As this Aura enters, choose a creature.\n" +
        "Enchanted creature is a copy of the chosen creature."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    replacementEffect(
        EntersWithChoice(
            ChoiceType.CREATURE_ON_BATTLEFIELD,
            permanentFilter = GameObjectFilter.Creature,
            then = Effects.EachPermanentBecomesCopyOfTarget(
                target = EffectTarget.ChosenCreature,
                affected = EffectTarget.EnchantedCreature,
                duration = Duration.WhileSourceAttached,
            ),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "60"
        artist = "Livia Prima"
        imageUri = "https://cards.scryfall.io/normal/front/e/f/eff03d37-d90a-4dcc-bacd-64fd71301354.jpg?1783934585"
        ruling("2018-07-13", "Metamorphic Alteration copies exactly what is printed on the chosen creature (unless that creature is copying something else or is a token). It doesn't copy whether that creature is tapped or untapped, whether it has any counters on it or any Auras and Equipment attached to it, or any non-copy effects that have changed its power, toughness, types, color, or so on.")
        ruling("2018-07-13", "The characteristics that will be copied are determined as Metamorphic Alteration's effect first begins to apply. If the copiable values of the chosen creature change later, or if that creature leaves the battlefield, the enchanted creature's characteristics are unaffected.")
        ruling("2018-07-13", "If Metamorphic Alteration somehow enters the battlefield at the same time as another creature, it can't have the enchanted creature become a copy of that creature. You may choose only a creature that's already on the battlefield.")
    }
}
