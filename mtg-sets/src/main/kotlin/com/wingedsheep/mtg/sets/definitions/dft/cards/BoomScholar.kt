package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReduceActivatedAbilityCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Boom Scholar — Aetherdrift #189
 * {1}{R}{G} · Creature — Goblin Advisor · 3/3
 *
 * Exhaust abilities of other permanents you control cost {2} less to activate.
 * Exhaust — {4}{R}{G}: Creatures and Vehicles you control gain trample until end of turn. Put two
 * +1/+1 counters on this creature.
 *
 * The discount is [ReduceActivatedAbilityCost] with `exhaustOnly = true` — the qualifier gates on the
 * *ability* (`isExhaust`, CR 702.177), not the permanent, so a matching permanent's ordinary
 * activated abilities stay at full price while its exhaust ability is {2} cheaper. `excludeSelf`
 * carries the printed "**other** permanents", so Boom Scholar's own {4}{R}{G} is never discounted.
 * `manaFloor` stays 0 per the 2025-02-07 ruling that the reduction *can* take the mana component
 * down to {0}; generic-only, so a `{R}{R}` exhaust cost is untouched (CR 118.7).
 *
 * "Creatures and Vehicles you control" is one union filter rather than two grants, and — as on its
 * set-mate Kickoff Celebrations — it is deliberately not narrowed to creatures: an uncrewed Vehicle
 * is a noncreature artifact that still picks up trample here, so crewing it later this turn yields a
 * trampler. Boom Scholar is itself a creature you control, so it gains trample from its own ability.
 */
val BoomScholar = card("Boom Scholar") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Goblin Advisor"
    power = 3
    toughness = 3
    oracleText = "Exhaust abilities of other permanents you control cost {2} less to activate.\n" +
        "Exhaust — {4}{R}{G}: Creatures and Vehicles you control gain trample until end of turn. " +
        "Put two +1/+1 counters on this creature. (Activate each exhaust ability only once.)"

    staticAbility {
        ability = ReduceActivatedAbilityCost(
            filter = GroupFilter(GameObjectFilter.Permanent.youControl(), excludeSelf = true),
            amount = DynamicAmount.Fixed(2),
            exhaustOnly = true
        )
    }

    activatedAbility {
        cost = Costs.Mana("{4}{R}{G}")
        isExhaust = true
        effect = Effects.Composite(
            Effects.ForEachInGroup(
                GroupFilter(
                    (GameObjectFilter.Creature or GameObjectFilter.Any.withSubtype("Vehicle")).youControl()
                ),
                Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.Self)
            ),
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
        )
        description = "Creatures and Vehicles you control gain trample until end of turn. Put two " +
            "+1/+1 counters on this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "189"
        artist = "Brian Valeza"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2b84684-10c9-4635-a922-620f04809bb1.jpg?1783907862"
        ruling(
            "2025-02-07",
            "Boom Scholar's first ability can cause the mana component of the cost to activate an " +
                "ability to be {0}."
        )
        ruling("2025-02-07", "Exhaust abilities can be activated any time you could normally activate an ability.")
        ruling(
            "2025-02-07",
            "If an exhaust ability of a permanent is activated, and then that permanent leaves the " +
                "battlefield and returns to the battlefield, it becomes a new object so its exhaust " +
                "ability can be activated again."
        )
        ruling(
            "2025-02-07",
            "If an ability triggers whenever you activate an exhaust ability, that ability resolves " +
                "before the exhaust ability resolves."
        )
    }
}
