package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Sharp-Eyed Rookie — Murders at Karlov Manor #176
 * {1}{G} · Creature — Human Detective · 2/2
 *
 * Vigilance
 * Whenever a creature you control enters, if its power is greater than this creature's power or
 * its toughness is greater than this creature's toughness, put a +1/+1 counter on this creature
 * and investigate.
 *
 * A self-escalating ramp payoff: every creature that out-sizes the Rookie grows it *and* draws
 * you a Clue, until it has outgrown the rest of your board.
 *
 * The trigger is `ANY`-bound with a `Creature.youControl()` filter rather than
 * [Triggers.OtherCreatureEnters] — the printed text says "a creature you control", not "another",
 * so the Rookie's own arrival does check itself. It never fires from that, because a creature's
 * power is never greater than its own.
 *
 * The "if …" clause is an intervening-if (CR 603.4): it gates whether the ability triggers at all
 * *and* is checked again on resolution, which `triggerCondition` provides. Both re-checks are
 * load-bearing per the printed rulings — two 3/3s entering against a 2/2 Rookie trigger twice, but
 * the second ability finds a 3/3 Rookie and does nothing. Either axis alone is enough, and the
 * axis may even swap between trigger and resolution (a 1/3 that gets +2/-2 in response still
 * resolves), which is exactly what a [Conditions.Any] over the two comparisons gives.
 *
 * Both sides read projected power/toughness, so +1/+1 counters an entering creature brings with
 * it, and lords on either side, count. If the entering creature has left by resolution the
 * comparison uses last-known information (CR 608.2h) — the same [EntityReference.Triggering] read
 * [HulklingBurgeoningBruiser] relies on.
 *
 * The counter and the Clue are one effect, not two abilities: they succeed or fail together.
 */
val SharpEyedRookie = card("Sharp-Eyed Rookie") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Detective"
    power = 2
    toughness = 2
    oracleText = "Vigilance\n" +
        "Whenever a creature you control enters, if its power is greater than this creature's " +
        "power or its toughness is greater than this creature's toughness, put a +1/+1 counter " +
        "on this creature and investigate. (Create a Clue token. It's an artifact with " +
        "\"{2}, Sacrifice this token: Draw a card.\")"

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Creature.youControl(),
            binding = TriggerBinding.ANY
        )
        triggerCondition = Conditions.Any(
            Compare(
                DynamicAmount.EntityProperty(EntityReference.Triggering, EntityNumericProperty.Power),
                ComparisonOperator.GT,
                DynamicAmount.EntityProperty(EntityReference.Source, EntityNumericProperty.Power)
            ),
            Compare(
                DynamicAmount.EntityProperty(EntityReference.Triggering, EntityNumericProperty.Toughness),
                ComparisonOperator.GT,
                DynamicAmount.EntityProperty(EntityReference.Source, EntityNumericProperty.Toughness)
            )
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
            .then(Effects.Investigate())
        description = "Whenever a creature you control enters, if its power is greater than this " +
            "creature's power or its toughness is greater than this creature's toughness, put a " +
            "+1/+1 counter on this creature and investigate."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "176"
        artist = "Jake Murray"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d5d4788-a970-4e09-89a1-740eca9331d9.jpg?1783912862"

        ruling(
            "2024-02-02",
            "When a creature enters the battlefield under your control, check its power and " +
                "toughness against Sharp-Eyed Rookie's power and toughness. If neither stat of the " +
                "new creature is greater, Sharp-Eyed Rookie's triggered ability won't trigger at all."
        )
        ruling(
            "2024-02-02",
            "If Sharp-Eyed Rookie's triggered ability triggers, the stat comparison will happen " +
                "again when the ability tries to resolve. If neither stat of the new creature is " +
                "greater, the ability will do nothing. If the creature that entered the battlefield " +
                "leaves the battlefield before the ability tries to resolve, use its power and " +
                "toughness as it last existed on the battlefield for the purposes of the comparison."
        )
        ruling(
            "2024-02-02",
            "If a creature enters the battlefield with +1/+1 counters on it, consider those " +
                "counters when determining if Sharp-Eyed Rookie's triggered ability will trigger."
        )
        ruling(
            "2024-02-02",
            "When comparing the stats as Sharp-Eyed Rookie's ability resolves, it's possible that " +
                "the stat that's greater changes from power to toughness or vice versa. If this " +
                "happens, the ability will still resolve and you'll put a +1/+1 counter on " +
                "Sharp-Eyed Rookie and investigate."
        )
    }
}
