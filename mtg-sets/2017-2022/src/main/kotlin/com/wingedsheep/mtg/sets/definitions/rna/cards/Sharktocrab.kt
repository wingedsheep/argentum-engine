package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sharktocrab — Ravnica Allegiance #206
 * {2}{G}{U} · Creature — Shark Octopus Crab · 4 / 4
 *
 * An `Effects.Adapt` activated ability (Aeromunculus), plus Herd Baloth's per-placement
 * "one or more +1/+1 counters are put on this creature" trigger driving Frost Lynx's
 * tap-and-lock: `Effects.Tap` then an `AbilityFlag.DOESNT_UNTAP` grant bounded by
 * [Duration.UntilAfterAffectedControllersNextUntap], so it expires against the *target's*
 * controller's untap step.
 */
val Sharktocrab = card("Sharktocrab") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Shark Octopus Crab"
    power = 4
    toughness = 4
    oracleText = "{2}{G}{U}: Adapt 1. (If this creature has no +1/+1 counters on it, put a +1/+1 counter on it.)\n" +
        "Whenever one or more +1/+1 counters are put on this creature, tap target creature an opponent controls. " +
        "That creature doesn't untap during its controller's next untap step."

    activatedAbility {
        cost = Costs.Mana("{2}{G}{U}")
        effect = Effects.Adapt(1)
        description = "{2}{G}{U}: Adapt 1."
    }

    triggeredAbility {
        trigger = Triggers.self.getsCounters(CounterType.PLUS_ONE_PLUS_ONE)
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.Tap(creature) then
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                creature,
                Duration.UntilAfterAffectedControllersNextUntap()
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "206"
        artist = "Jehan Choo"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0f31228-0c47-4c51-932d-50fdec965f15.jpg?1783933636"
        ruling("2019-01-25", "Sharktocrab's triggered ability can target a creature that's already tapped. That creature won't untap during its controller's next untap step.")
        ruling("2019-01-25", "An ability that triggers when counters are put on a permanent will trigger if that permanent somehow enters the battlefield with those counters.")
        ruling("2019-01-25", "You can always activate an ability that will cause a creature to adapt. As that ability resolves, if the creature has a +1/+1 counter on it for any reason, you simply won't put any +1/+1 counters on it.")
        ruling("2019-01-25", "If a creature somehow loses all of its +1/+1 counters, it can adapt again and get more +1/+1 counters.")
    }
}
