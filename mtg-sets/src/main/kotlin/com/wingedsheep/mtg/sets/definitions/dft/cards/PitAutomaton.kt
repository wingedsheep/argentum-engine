package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pit Automaton — Aetherdrift #238
 * {2} · Artifact Creature — Construct · 0/4
 *
 * Defender
 * {T}: Add {C}{C}. Spend this mana only to activate abilities.
 * {2}, {T}: When you next activate an exhaust ability that isn't a mana ability this turn, copy it.
 * You may choose new targets for the copy.
 *
 * Modeling notes:
 *  - The mana ability is the unqualified "only to activate abilities" restriction
 *    ([ManaRestriction.AbilityActivationOnly]) that Guidelight Optimizer and Redshift also use — it
 *    constrains what each pip may pay for, not how many abilities it feeds, so the two {C} can be
 *    split across activations and comfortably cover an exhaust ability's cost.
 *  - "When you **next** … this turn" is a one-shot delayed triggered ability, so the payoff is
 *    [CreateDelayedTriggerEffect] with `fireOnce = true` and [DelayedTriggerExpiry.EndOfTurn]: the
 *    first matching activation consumes it, and an unfired one is swept at end of turn. Activating
 *    this ability twice stacks two independent delayed triggers, so the next exhaust ability is
 *    copied twice — matching the rules, since each resolution creates its own delayed ability.
 *  - The trigger is [Triggers.YouActivateNonManaExhaustAbility], not the plain
 *    [Triggers.YouActivateExhaustAbility] the other Aetherdrift exhaust payoffs use. Pit Automaton's
 *    Oracle text was updated on release to add "that isn't a mana ability", so an exhaust *mana*
 *    ability must not arm the copy.
 *  - The copy itself reuses the shared [Effects.CopyTargetSpellOrAbility] against
 *    [EffectTarget.TriggeringEntity] — the activated ability still on the stack beneath this delayed
 *    trigger — which prompts for new targets per CR 707.10c. Per the printed ruling the triggered
 *    ability resolves *before* the exhaust ability it copied, so the ability is guaranteed to still
 *    be on the stack; that ordering is what makes `TriggeringEntity` resolvable at fire time (the
 *    delayed-trigger executor deliberately leaves the copy effect's target unbaked).
 */
val PitAutomaton = card("Pit Automaton") {
    manaCost = "{2}"
    typeLine = "Artifact Creature — Construct"
    power = 0
    toughness = 4
    oracleText = "Defender\n" +
        "{T}: Add {C}{C}. Spend this mana only to activate abilities.\n" +
        "{2}, {T}: When you next activate an exhaust ability that isn't a mana ability this turn, " +
        "copy it. You may choose new targets for the copy."

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(2, ManaRestriction.AbilityActivationOnly)
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add {C}{C}. Spend this mana only to activate abilities."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = CreateDelayedTriggerEffect(
            trigger = Triggers.YouActivateNonManaExhaustAbility,
            effect = Effects.CopyTargetSpellOrAbility(EffectTarget.TriggeringEntity),
            fireOnce = true,
            expiry = DelayedTriggerExpiry.EndOfTurn
        )
        description = "When you next activate an exhaust ability that isn't a mana ability this " +
            "turn, copy it. You may choose new targets for the copy."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "238"
        artist = "Villarrte"
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c72527ef-ac05-44c8-8c76-10532ce3da6e.jpg?1783907847"
        ruling(
            "2025-02-07",
            "Pit Automaton has received an update to its Oracle text. Specifically, the delayed " +
                "triggered ability doesn't trigger if you activate an exhaust ability that is also " +
                "a mana ability."
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
