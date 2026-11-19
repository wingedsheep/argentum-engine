package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hero of Iroas — Born of the Gods #17 (canonical printing)
 * {1}{W}
 * Creature — Human Soldier
 * 2/2
 * Aura spells you cast cost {1} less to cast.
 * Heroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on this creature.
 *
 * The discount is Transcendent Envoy's Aura-keyed `ModifySpellCost`; a bestow cast is priced with
 * its announced Aura characteristics, so bestow costs are discounted too (the 2014 ruling).
 * Heroic is Favored Hoplite's `TargetsSource` cast trigger.
 */
val HeroOfIroas = card("Hero of Iroas") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "Aura spells you cast cost {1} less to cast.\n" +
        "Heroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on this creature."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any.withSubtype("Aura")),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Heroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "17"
        artist = "Willian Murai"
        flavorText = "\"My left has felled a cyclops. My right has slain a giant. I bring both to every fight.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/9/7917f2ee-f093-4ffb-83ed-a181b14b5957.jpg?1783939579"
        ruling("2018-12-07", "To determine the total cost of a spell, start with the mana cost or alternative cost you're paying, add any cost increases, then apply any cost reductions. The mana value of the spell remains unchanged, no matter what the total cost to cast it was.")
        ruling("2018-12-07", "Hero of Iroas's first ability reduces only generic mana in the spell's total cost.")
        ruling("2014-02-01", "Hero of Iroas's first ability will apply if you cast a card for its bestow cost.")
        ruling("2013-09-15", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2013-09-15", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2013-09-15", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
