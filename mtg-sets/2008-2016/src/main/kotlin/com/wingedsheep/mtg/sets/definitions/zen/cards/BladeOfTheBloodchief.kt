package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Blade of the Bloodchief
 * {1}
 * Artifact — Equipment
 * Whenever a creature dies, put a +1/+1 counter on equipped creature. If equipped creature is a
 * Vampire, put two +1/+1 counters on it instead.
 * Equip {1}
 *
 * Any creature dying triggers it, whoever controlled it (ruling). The counters go to the creature
 * the Blade is attached to as the ability resolves — or, if the Blade has left the battlefield, the
 * creature it was attached to when it left (ruling); both [EffectTarget.EquippedCreature] and the
 * "is a Vampire" check read that last-known host. The "instead" is an in-ability choice of count
 * (one or two counters in a single placement), not a replacement effect. Unattached, it does nothing.
 */
val BladeOfTheBloodchief = card("Blade of the Bloodchief") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Whenever a creature dies, put a +1/+1 counter on equipped creature. If equipped creature " +
        "is a Vampire, put two +1/+1 counters on it instead.\n" +
        "Equip {1}"

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature).dies()
        effect = Effects.If(
            condition = Conditions.EntityMatches(
                EffectTarget.EquippedCreature,
                GameObjectFilter.Creature.withSubtype(Subtype.VAMPIRE),
            ),
            then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.EquippedCreature),
            otherwise = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.EquippedCreature),
        )
        description = "Whenever a creature dies, put a +1/+1 counter on equipped creature. If equipped " +
            "creature is a Vampire, put two +1/+1 counters on it instead."
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "196"
        artist = "Jung Park"
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e4b42e5-0e60-4148-907e-456d9f3043be.jpg?1783942128"
        ruling("2009-10-01", "Blade of the Bloodchief's ability triggers regardless of who controlled the creature and whose graveyard it was put into.")
        ruling("2009-10-01", "The +1/+1 counters are put on the equipped creature, not Blade of the Bloodchief. If Blade of the Bloodchief is moved to a different creature, the +1/+1 counters will stay where they are.")
        ruling("2009-10-01", "The creature that gets the counters is the one that's equipped by Blade of the Bloodchief at the time the ability resolves. It doesn't matter what creature Blade of the Bloodchief was attached to (if any) when it triggered.")
        ruling("2009-10-01", "If Blade of the Bloodchief leaves the battlefield before its triggered ability resolves, the counters are put on the creature it was attached to at the time it left the battlefield. If it wasn't attached to a creature at that time, nothing gets the counters.")
        ruling("2009-10-01", "If the equipped creature itself is put into a graveyard, Blade of the Bloodchief's ability will trigger. However, it won't do anything when it resolves unless a spell or ability has caused it to become attached to another creature by that time. (Remember that you may activate an equip ability only as a sorcery.)")
        ruling("2009-10-01", "If the equipped creature and another creature are dealt lethal damage at the same time, the equipped creature will be put into a graveyard before it would receive any counters.")
    }
}
