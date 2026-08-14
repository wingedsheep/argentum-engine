package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Wurmskin Forger — Mirrodin #140
 * {5}{G}{G} · Creature — Elf Warrior · 2/2
 *
 * When this creature enters, distribute three +1/+1 counters among one, two, or three target
 * creatures.
 *
 * The distribute shape: `TargetCreature(count = 3, minCount = 1)` is exactly "one, two, or
 * three target creatures" — the caster declares between one and three of them as the trigger
 * goes on the stack, and [Effects.DistributeCountersAmongTargets] splits the fixed pool of
 * three counters across whatever survived to resolution (CR 601.2d: the division is announced
 * with the targets, each declared target must get at least one counter).
 *
 * Unlike the "you control" members of this family
 * ([Jade Seedstones][com.wingedsheep.mtg.sets.definitions.lci.cards.JadeSeedstones],
 * Armament Corps), the printed text has **no controller restriction** — the default
 * `TargetFilter.Creature` is correct, so an opponent's creature is a legal recipient. The
 * Forger itself is on the battlefield when its own enters-trigger resolves, so it can be one
 * of the three.
 */
val WurmskinForger = card("Wurmskin Forger") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Warrior"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, distribute three +1/+1 counters among one, two, " +
        "or three target creatures."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target = TargetCreature(count = 3, minCount = 1)
        effect = Effects.DistributeCountersAmongTargets(totalCounters = 3)
        description = "When this creature enters, distribute three +1/+1 counters among one, " +
            "two, or three target creatures."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "140"
        artist = "Justin Sweet"
        flavorText = "It takes three weeks for a patrol of hunters to down a slagwurm. It takes " +
            "just as long to make a single cut in its hide."
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f14f303e-371b-47dd-bbb8-7f3e44ef9af0.jpg?1783944529"
    }
}
