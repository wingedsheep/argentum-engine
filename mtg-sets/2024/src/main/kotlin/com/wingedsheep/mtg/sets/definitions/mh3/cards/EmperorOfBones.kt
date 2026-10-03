package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Emperor of Bones {1}{B}
 * Creature — Skeleton Noble
 * 2/2
 * At the beginning of combat on your turn, exile up to one target card from a graveyard.
 * {1}{B}: Adapt 2.
 * Whenever one or more +1/+1 counters are put on this creature, put a creature card exiled with
 * this creature onto the battlefield under your control with a finality counter on it. It gains
 * haste. Sacrifice it at the beginning of the next end step.
 *
 * The begin-combat exile is linked to the Emperor (`linkToSource`), so the counters trigger can
 * gather that pile with `CardSource.FromLinkedExile()`. The trigger fires on +1/+1 counters from
 * any source, not just its own adapt. The reanimated creature enters with a finality counter
 * (engine-wide "would go to a graveyard → exile instead"), gains haste with no duration, and a
 * delayed end-step trigger sacrifices it (only if it's still that same object).
 */
val EmperorOfBones = card("Emperor of Bones") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Skeleton Noble"
    oracleText = "At the beginning of combat on your turn, exile up to one target card from a graveyard.\n" +
        "{1}{B}: Adapt 2. (If this creature has no +1/+1 counters on it, put two +1/+1 counters on it.)\n" +
        "Whenever one or more +1/+1 counters are put on this creature, put a creature card exiled with " +
        "this creature onto the battlefield under your control with a finality counter on it. It gains " +
        "haste. Sacrifice it at the beginning of the next end step. (If a creature with a finality " +
        "counter on it would die, exile it instead.)"
    power = 2
    toughness = 2

    // At the beginning of combat on your turn, exile up to one target card from a graveyard.
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        val t = target(TargetFilter.CardInGraveyard, optional = true)
        effect = Effects.Move(t, Zone.EXILE, linkToSource = true)
        description = "Exile up to one target card from a graveyard."
    }

    // {1}{B}: Adapt 2.
    activatedAbility {
        cost = Costs.Mana("{1}{B}")
        effect = Effects.If(
            Conditions.Not(Conditions.SourceHasCounter(CounterType.PLUS_ONE_PLUS_ONE)),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self),
        )
        description = "Adapt 2."
    }

    // Whenever one or more +1/+1 counters are put on this creature, reanimate a linked-exiled
    // creature card with a finality counter; it gains haste and is sacrificed at the next end step.
    triggeredAbility {
        trigger = Triggers.self.getsCounters(CounterType.PLUS_ONE_PLUS_ONE)
        effect = Effects.Pipeline {
            val pile = gather(CardSource.FromLinkedExile())
            val creatures = filter(pile, GameObjectFilter.Creature)
            val chosen = chooseExactly(
                1,
                from = creatures,
                prompt = "Choose a creature card exiled with Emperor of Bones to put onto the battlefield",
            )
            val entered = moveTracked(
                chosen,
                CardDestination.ToZone(Zone.BATTLEFIELD, Player.You),
                addCounterType = CounterType.FINALITY,
            )
            ifNotEmpty(entered) {
                run(Effects.GrantKeyword(
                    keyword = Keyword.HASTE,
                    target = entered.asTarget,
                    duration = Duration.Permanent,
                ))
                run(Effects.CreateDelayedTrigger(
                    step = Step.END,
                    effect = Effects.SacrificeTarget(entered.asTarget),
                ))
            }
        }
        description = "Put a creature card exiled with this creature onto the battlefield under your " +
            "control with a finality counter on it. It gains haste. Sacrifice it at the beginning of " +
            "the next end step."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "90"
        artist = "Josh Hass"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df9d9075-2d1e-4848-b661-816d539e05eb.jpg?1783911281"
        ruling("2024-06-07", "If any permanent with a finality counter on it would go to a graveyard from the battlefield, exile it instead.")
        ruling("2024-06-07", "You can always activate an ability that will cause a creature to adapt. As that ability resolves, if the creature has a +1/+1 counter on it for any reason, you simply won't put any +1/+1 counters on it.")
    }
}
