package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * The Mind Stone — Marvel Super Heroes #21 (mythic)
 * {1}{W} · Legendary Artifact — Infinity Stone
 *
 * Indestructible
 * {T}: Add {W}.
 * {5}{W}, {T}: Harness The Mind Stone. (Once harnessed, its ∞ ability is active.)
 * ∞ — At the beginning of your end step, exile up to one other target nonland permanent you
 * control, then return that card to the battlefield under its owner's control.
 *
 * Same shape as the already-modeled Infinity Stone cycle (see `spm/cards/TheSoulStone.kt`):
 *  - **Harness** is the binary marker counter [Counters.HARNESS]. The Harness activated ability
 *    places one; the `∞` triggered ability carries
 *    [Conditions.SourceHasCounter] over it, so it does nothing until the Stone is harnessed and
 *    fires every end step thereafter. As an intervening-if condition it is re-checked on
 *    resolution, and the counter is lost if the Stone leaves the battlefield.
 *  - The blink is the standard [Effects.Move] to exile `.then` back to the battlefield
 *    (Restoration Angel's idiom). The returned permanent is a new object that enters untapped,
 *    without counters/Auras, under its **owner's** control — which is what the default (no
 *    `controllerOverride`) [Effects.Move] to [Zone.BATTLEFIELD] does.
 *  - "up to one **other** target nonland permanent you control" is an optional
 *    [TargetPermanent] over `NonlandPermanent.youControl().other()` — `.other()` sets
 *    `excludeSelf`, so the Stone can't blink itself.
 */
val TheMindStone = card("The Mind Stone") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Artifact — Infinity Stone"
    oracleText = "Indestructible\n" +
        "{T}: Add {W}.\n" +
        "{5}{W}, {T}: Harness The Mind Stone. (Once harnessed, its ∞ ability is active.)\n" +
        "∞ — At the beginning of your end step, exile up to one other target nonland permanent " +
        "you control, then return that card to the battlefield under its owner's control."

    keywords(Keyword.INDESTRUCTIBLE)

    // {T}: Add {W}.
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
    }

    // {5}{W}, {T}: Harness The Mind Stone.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}{W}"), Costs.Tap)
        effect = Effects.AddCounters(Counters.HARNESS, 1, EffectTarget.Self)
        description = "{5}{W}, {T}: Harness The Mind Stone."
    }

    // ∞ — At the beginning of your end step (once harnessed), exile up to one other target
    // nonland permanent you control, then return that card to the battlefield.
    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.SourceHasCounter(CounterTypeFilter.Named(Counters.HARNESS))
        val permanent = target(
            "up to one other target nonland permanent you control",
            TargetPermanent(
                optional = true,
                filter = TargetFilter.NonlandPermanent.youControl().other(),
            ),
        )
        effect = Effects.Move(permanent, Zone.EXILE)
            .then(Effects.Move(permanent, Zone.BATTLEFIELD))
        description = "∞ — At the beginning of your end step, exile up to one other target " +
            "nonland permanent you control, then return that card to the battlefield under its " +
            "owner's control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "21"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87f1e69a-6d74-4982-afda-82613637799a.jpg?1783902972"
    }
}
