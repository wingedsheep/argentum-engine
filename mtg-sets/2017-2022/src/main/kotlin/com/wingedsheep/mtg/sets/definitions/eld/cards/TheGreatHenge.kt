package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty

/**
 * The Great Henge
 * {7}{G}{G}
 * Legendary Artifact
 *
 * This spell costs {X} less to cast, where X is the greatest power among creatures you control.
 * {T}: Add {G}{G}. You gain 2 life.
 * Whenever a nontoken creature you control enters, put a +1/+1 counter on it and draw a card.
 *
 * The cost reduction is the same self-cast [ModifySpellCost] Molten Monstrosity and The Skullspore
 * Nexus use: it reduces only the generic part (the {G}{G} must still be paid) and reads projected
 * power of creatures you control.
 */
val TheGreatHenge = card("The Great Henge") {
    manaCost = "{7}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Artifact"
    oracleText = "This spell costs {X} less to cast, where X is the greatest power among creatures you control.\n" +
        "{T}: Add {G}{G}. You gain 2 life.\n" +
        "Whenever a nontoken creature you control enters, put a +1/+1 counter on it and draw a card."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.GreatestPropertyAmongPermanentsYouControl(
                    EntityNumericProperty.Power, Filters.Creature
                )
            ),
        )
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN, 2) then Effects.GainLife(2)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.nontoken().youControl()).enters()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.TriggeringEntity) then
            Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "161"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af915ed2-1f34-43f6-85f5-2430325b720f.jpg?1783932609"
        ruling(
            "2025-10-02",
            "The first step of casting a spell is to move it to the stack. If this causes the greatest power " +
                "among creatures you control to change, that new power will be used to determine the cost reduction."
        )
        ruling(
            "2025-10-02",
            "The cost reduction ability reduces only the generic mana in The Great Henge's cost. The colored mana must still be paid."
        )
        ruling(
            "2025-10-02",
            "Once The Great Henge's last ability has triggered, you'll draw a card even if you can't put a +1/+1 " +
                "counter on the creature for some reason (most likely because it has left the battlefield)."
        )
    }
}
