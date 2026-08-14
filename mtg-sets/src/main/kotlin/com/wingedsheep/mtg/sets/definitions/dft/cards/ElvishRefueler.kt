package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.IgnoreExhaustActivationLimit
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Elvish Refueler — Aetherdrift #161
 * {2}{G} · Creature — Elf Druid · 2/3
 *
 * During your turn, as long as you haven't activated an exhaust ability this turn, you may activate
 * exhaust abilities as though they haven't been activated.
 * Exhaust — {1}{G}: Put a +1/+1 counter on this creature.
 *
 * Modeling notes:
 *  - The permission is [IgnoreExhaustActivationLimit]: it waives the "activate only once" memory
 *    (CR 702.177a) that `isExhaust = true` installs as an
 *    [com.wingedsheep.sdk.scripting.ActivationRestriction.Once], and only for exhaust abilities — a
 *    non-exhaust ability with a printed `Once`/`OncePerTurn` restriction is untouched.
 *  - Its gate is the printed conjunction: [Conditions.IsYourTurn] AND
 *    [Conditions.YouHaventActivatedAnExhaustAbilityThisTurn], the latter reading the per-player
 *    `ExhaustAbilitiesActivatedThisTurnComponent` the engine bumps on every exhaust activation. The
 *    count advances for the waived activation too, so the net effect is what the card means: once
 *    per turn, on your turn, you get to re-use one already-spent exhaust ability — including the
 *    Refueler's own, and including a *different* permanent's.
 *  - The condition is re-evaluated at activation-legality time on every frame rather than projected
 *    as a continuous effect, so the permission disappears mid-turn the instant the count moves,
 *    and an opponent's turn never sees it.
 */
val ElvishRefueler = card("Elvish Refueler") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid"
    power = 2
    toughness = 3
    oracleText = "During your turn, as long as you haven't activated an exhaust ability this turn, " +
        "you may activate exhaust abilities as though they haven't been activated.\n" +
        "Exhaust — {1}{G}: Put a +1/+1 counter on this creature. " +
        "(Activate each exhaust ability only once.)"

    staticAbility {
        ability = IgnoreExhaustActivationLimit(
            condition = Conditions.All(
                Conditions.IsYourTurn,
                Conditions.YouHaventActivatedAnExhaustAbilityThisTurn
            )
        )
    }

    activatedAbility {
        cost = Costs.Mana("{1}{G}")
        isExhaust = true
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Exhaust — {1}{G}: Put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "161"
        artist = "Carly Milligan"
        flavorText = "\"Anyone running low?\""
        imageUri = "https://cards.scryfall.io/normal/front/2/5/25dfbcb6-9b67-4151-b10f-dde70c5fd16d.jpg?1783907872"
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
