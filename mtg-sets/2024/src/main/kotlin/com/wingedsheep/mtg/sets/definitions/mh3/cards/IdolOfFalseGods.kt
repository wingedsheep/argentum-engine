package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Idol of False Gods — Modern Horizons 3 #210 (uncommon)
 * {2} · Kindred Artifact — Eldrazi
 *
 * {1}{C}, {T}: Create a 0/1 colorless Eldrazi Spawn creature token with "Sacrifice this token: Add {C}."
 * Whenever another Eldrazi you control dies, put a +1/+1 counter on this artifact.
 * As long as this artifact has eight or more +1/+1 counters on it, it's a 0/0 creature in addition
 * to its other types and it has annihilator 2.
 *
 * Modelling notes:
 *  - "Another Eldrazi" is a bare tribal noun, so it is a *permanent* filter: the ruling confirms a
 *    noncreature Eldrazi (e.g. a kindred artifact) going to the graveyard also triggers it.
 *  - The animation is one multi-layer static gated on the counter count: Layer 4 adds CREATURE and
 *    Layer 7b sets base P/T 0/0 (the counters then make it N/N). Annihilator 2 is lowered separately.
 *  - Annihilator is display-only in the SDK, so it is lowered to its attack trigger (as on
 *    Nulldrifter). "It has annihilator 2" only while the condition holds, so the trigger carries
 *    the condition as a trigger-time restriction, not an intervening-if: once triggered it resolves
 *    even if the Idol has since lost counters.
 */
val IdolOfFalseGods = card("Idol of False Gods") {
    manaCost = "{2}"
    typeLine = "Kindred Artifact — Eldrazi"
    oracleText = "{1}{C}, {T}: Create a 0/1 colorless Eldrazi Spawn creature token with " +
        "\"Sacrifice this token: Add {C}.\"\n" +
        "Whenever another Eldrazi you control dies, put a +1/+1 counter on this artifact.\n" +
        "As long as this artifact has eight or more +1/+1 counters on it, it's a 0/0 creature in " +
        "addition to its other types and it has annihilator 2."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{C}"), Costs.Tap)
        effect = Effects.CreateEldraziSpawn()
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Permanent.withSubtype("Eldrazi").youControl()).dies()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    // One multi-layer static (CR 613.6): it began applying in Layer 4, so losing all abilities
    // later (Layer 6) doesn't switch off its 0/0 — the Idol stays an N/N creature.
    staticAbility {
        condition = Conditions.SourceCounterCountAtLeast(CounterType.PLUS_ONE_PLUS_ONE, 8)
        ability = CompositeStaticAbility(
            listOf(
                GrantCardType("CREATURE", GroupFilter.source()),
                SetBasePowerToughnessStatic(0, 0, GroupFilter.source()),
            )
        )
    }

    keywordAbility(KeywordAbility.annihilator(2))

    // Annihilator 2 — the lowering of the display-only keyword, live only at 8+ counters.
    triggeredAbility {
        trigger = Triggers.self.attacks()
        triggerRestriction = Conditions.SourceCounterCountAtLeast(CounterType.PLUS_ONE_PLUS_ONE, 8)
        effect = Effects.Sacrifice(
            GameObjectFilter.Permanent,
            2,
            EffectTarget.PlayerRef(Player.DefendingPlayer)
        )
        description = "Annihilator 2"
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "210"
        artist = "Nicholas Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d5e1ff6d-190a-497f-89c3-35e0979c93b5.jpg?1783911245"
        ruling(
            "2024-06-07",
            "Idol of False Gods's second ability will trigger whenever another Eldrazi you control is " +
                "put into a graveyard from the battlefield, even if that Eldrazi isn't a creature."
        )
        ruling(
            "2024-06-07",
            "Annihilator abilities trigger and resolve during the declare attackers step. The defending " +
                "player sacrifices the required number of permanents of their choice before they declare " +
                "blockers. Any creatures sacrificed this way won't be able to block."
        )
    }
}
