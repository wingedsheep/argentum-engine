package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lagrella, the Magpie — Streets of New Capenna #196
 * {G}{W}{U} · Legendary Creature — Human Soldier · 2/3
 *
 * When Lagrella enters, exile any number of other target creatures controlled by different
 * players until Lagrella leaves the battlefield. When an exiled card enters under your control
 * this way, put two +1/+1 counters on it.
 *
 * - The exile is one instance of "target" with no upper bound but the cross-target
 *   `differentControllers` rule (at most one creature per controller), each target exiled by
 *   [Effects.ExileUntilLeaves] — a no-op if Lagrella already left before the trigger resolved.
 * - The return is the usual linked leaves-the-battlefield trigger, under each card's owner's
 *   control (CR 610.3c). Only the cards you own come back under your control, so only they earn
 *   the counters.
 * - "When an exiled card enters under your control this way" is a separate triggered ability
 *   (CR 603.12-style, triggered by the return), so it's a [Effects.ReflexiveTrigger]: the action
 *   half returns the pile and records the returned permanents you control under [YOURS], the
 *   reflexive half goes on the stack and puts the counters on that collection. It's only built
 *   when at least one exiled card is yours, so a pile of opponents' creatures returns without
 *   an empty trigger.
 */
private const val YOURS = "lagrellaReturnedYours"

private val returnExiledToOwners: Effect = Effects.Pipeline {
    val pile = gather(CardSource.FromLinkedExile())
    move(pile, CardDestination.ToZone(Zone.BATTLEFIELD), underOwnersControl = true)
}

private val returnAndCounterYours: Effect = Effects.ReflexiveTrigger(
    optional = false,
    action = Effects.Pipeline {
        val pile = gather(CardSource.FromLinkedExile())
        val returned = moveTracked(pile, CardDestination.ToZone(Zone.BATTLEFIELD), underOwnersControl = true)
        filter(returned, GameObjectFilter.Any.youControl(), name = YOURS)
    },
    reflexiveEffect = Effects.AddCountersToCollection(YOURS, CounterType.PLUS_ONE_PLUS_ONE, 2),
    descriptionOverride = "Return the cards exiled with Lagrella to the battlefield under their " +
        "owners' control. When an exiled card enters under your control this way, put two " +
        "+1/+1 counters on it"
)

val LagrellaTheMagpie = card("Lagrella, the Magpie") {
    manaCost = "{G}{W}{U}"
    colorIdentity = "GWU"
    typeLine = "Legendary Creature — Human Soldier"
    oracleText = "When Lagrella enters, exile any number of other target creatures controlled by different players until Lagrella leaves the battlefield. When an exiled card enters under your control this way, put two +1/+1 counters on it."
    power = 2
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(
            TargetFilter.OtherCreature,
            unlimited = true,
            differentControllers = true,
        )
        effect = Effects.ForEachTarget(Effects.ExileUntilLeaves(EffectTarget.ContextTarget(0)))
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.Pipeline {
            val pile = gather(CardSource.FromLinkedExile())
            ifNotEmpty(pile, filter = GameObjectFilter.Any.ownedByYou()) {
                run(returnAndCounterYours)
            } orElse {
                run(returnExiledToOwners)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "196"
        artist = "Donato Giancola"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd20f271-26c8-429f-a422-e7d95f3bda74.jpg?1783923080"
    }
}
