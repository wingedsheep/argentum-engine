package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Afterburner Expert — Aetherdrift #150
 * {2}{G} · Creature — Goblin Artificer · 4/2
 *
 * Its second ability functions from the graveyard. It triggers as soon as any exhaust ability you
 * control is activated, so the Expert returns before that exhaust ability resolves.
 */
val AfterburnerExpert = card("Afterburner Expert") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Goblin Artificer"
    power = 4
    toughness = 2
    oracleText = "Exhaust — {2}{G}{G}: Put two +1/+1 counters on this creature. " +
        "(Activate each exhaust ability only once.)\n" +
        "Whenever you activate an exhaust ability, return this card from your graveyard to the battlefield."

    activatedAbility {
        cost = Costs.Mana("{2}{G}{G}")
        isExhaust = true
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
        description = "Exhaust — {2}{G}{G}: Put two +1/+1 counters on this creature."
    }

    triggeredAbility {
        triggerZone = Zone.GRAVEYARD
        trigger = Triggers.YouActivateExhaustAbility
        effect = Effects.Move(EffectTarget.Self, Zone.BATTLEFIELD)
        description = "Whenever you activate an exhaust ability, return this card from your " +
            "graveyard to the battlefield."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "150"
        artist = "April Prime"
        flavorText = "Speedbump has miraculously survived ninety-nine rocket crashes, and he's " +
            "aiming for an even hundred."
        imageUri = "https://cards.scryfall.io/normal/front/5/5/555e1bfc-6d07-4979-a914-b2bd1fb031f2.jpg?1783907875"
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
