package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Goblin-town — The Hobbit #183
 * Land · Common
 *
 * This land enters tapped.
 * {T}: Add {B} or {R}.
 * {2}{B}{R}, {T}, Sacrifice this land: Put two +1/+1 counters on target Goblin or Orc you control.
 * Activate only as a sorcery.
 *
 * The Goblin/Orc member of the HOB tapland cycle; see [IronHills] for the shape. The two-tribe
 * clause is one target with an OR over subtypes ([GameObjectFilter.withAnySubtype]), not two
 * targets — a creature that is both still only satisfies the single requirement once.
 */
val GoblinTown = card("Goblin-town") {
    manaCost = ""
    colorIdentity = "BR"
    typeLine = "Land"
    oracleText = "This land enters tapped.\n" +
        "{T}: Add {B} or {R}.\n" +
        "{2}{B}{R}, {T}, Sacrifice this land: Put two +1/+1 counters on target Goblin or Orc you " +
        "control. Activate only as a sorcery."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}{R}"), Costs.Tap, Costs.SacrificeSelf)
        val goblinOrOrc = target(
            "target Goblin or Orc you control",
            TargetCreature(
                filter = TargetFilter(
                    GameObjectFilter.Creature.youControl().withAnySubtype("Goblin", "Orc")
                )
            )
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, goblinOrOrc)
        timing = TimingRule.SorcerySpeed
        description = "Put two +1/+1 counters on target Goblin or Orc you control."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "183"
        artist = "Sean Vo"
        flavorText = "The passages were crossed and tangled in all directions, and it was most " +
            "horribly stuffy."
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d76df9d0-56cf-4351-a5e8-e6ae6fc791d1.jpg?1785323613"
    }
}
