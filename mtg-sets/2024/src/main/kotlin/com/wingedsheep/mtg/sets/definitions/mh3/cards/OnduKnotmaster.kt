package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ondu Knotmaster // Throw a Line (MH3 #196)
 * {2}{W}{B}
 * Creature — Kor Rogue
 * 2/2
 *
 * Lifelink
 * Whenever another modified creature you control dies, put two +1/+1 counters on this creature.
 *
 * Adventure: Throw a Line — {W}{B}, Sorcery — Adventure
 * Distribute two +1/+1 counters among one or two target creatures.
 *
 * "Modified" is [StatePredicate.IsModified]; on a dies trigger the matcher answers it from the
 * departing creature's last-known information (counters, Equipment, Auras its controller controls).
 */
private val modifiedCreatureYouControl = GameObjectFilter.Creature.youControl().let {
    it.copy(statePredicates = it.statePredicates + StatePredicate.IsModified)
}

val OnduKnotmaster = card("Ondu Knotmaster") {
    manaCost = "{2}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Kor Rogue"
    power = 2
    toughness = 2
    oracleText = "Lifelink\n" +
        "Whenever another modified creature you control dies, put two +1/+1 counters on this creature."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.another(modifiedCreatureYouControl).dies()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
        description = "Whenever another modified creature you control dies, put two +1/+1 counters on this creature."
    }

    adventure("Throw a Line") {
        manaCost = "{W}{B}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Distribute two +1/+1 counters among one or two target creatures. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            targets(TargetFilter.Creature, count = 2, minCount = 1)
            effect = Effects.DistributeCountersAmongTargets(totalCounters = 2)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "196"
        artist = "Jarel Threat"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/27912e61-97ef-406b-bb88-4fe89a54726e.jpg?1783911248"
        ruling("2024-06-07", "You choose how the counters will be distributed as you cast Throw a Line. Each target must receive at least one +1/+1 counter.")
        ruling("2024-06-07", "If you choose two targets for Throw a Line, and one of the creatures is an illegal target as Throw a Line tries to resolve, the original distribution of counters still applies and the counter that would have been put on the illegal target are lost.")
        ruling("2024-06-07", "An Aura controlled by another player does not cause a creature you control to be modified.")
        ruling("2024-06-07", "A creature with a counter on it is considered modified no matter what kind of counter it is or which player put it on that creature.")
    }
}
