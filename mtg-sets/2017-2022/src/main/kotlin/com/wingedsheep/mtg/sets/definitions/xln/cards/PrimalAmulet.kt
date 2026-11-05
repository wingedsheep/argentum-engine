package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaSpellRider
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ManaColorSet

/**
 * Primal Amulet // Primal Wellspring (Ixalan — the card's earliest printing)
 * {4}
 * Artifact // Land
 *
 * Front — Primal Amulet (Artifact, {4})
 *   Instant and sorcery spells you cast cost {1} less to cast.
 *   Whenever you cast an instant or sorcery spell, put a charge counter on this artifact. Then if
 *   there are four or more charge counters on it, you may remove those counters and transform it.
 *
 * Back — Primal Wellspring (Land)
 *   {T}: Add one mana of any color. When that mana is spent to cast an instant or sorcery spell,
 *   copy that spell and you may choose new targets for the copy.
 *
 * Implementation:
 *  - The discount is Goblin Electromancer's [ModifySpellCost] on `YouCast(InstantOrSorcery)`.
 *  - The cast trigger adds the counter, then an [Effects.If] on
 *    [Conditions.SourceCounterCountAtLeast] checked at resolution (so a fourth counter from
 *    elsewhere waits for the next cast — ruling) wraps an optional "remove those counters and
 *    transform it". "Those counters" is all of them, read as the counter count on Self.
 *  - Primal Wellspring is Pyromancer's Goggles' [ManaSpellRider.TriggersWhenSpent] copy rider on
 *    any-color mana instead of {R}: the mana stays unrestricted, the rider fires per mana spent
 *    on an instant or sorcery, and the trigger is the spell's, not the land's.
 */

private val PrimalAmuletFront = card("Primal Amulet") {
    manaCost = "{4}"
    typeLine = "Artifact"
    oracleText = "Instant and sorcery spells you cast cost {1} less to cast.\n" +
        "Whenever you cast an instant or sorcery spell, put a charge counter on this artifact. " +
        "Then if there are four or more charge counters on it, you may remove those counters " +
        "and transform it."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.InstantOrSorcery),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        effect = Effects.AddCounters(CounterType.CHARGE, 1, EffectTarget.Self) then
            Effects.If(
                condition = Conditions.SourceCounterCountAtLeast(CounterType.CHARGE, 4),
                then = Effects.May(
                    Effects.RemoveCounters(
                        CounterType.CHARGE,
                        DynamicAmounts.countersOnSelf(CounterType.CHARGE),
                        EffectTarget.Self,
                    ) then Effects.Transform(EffectTarget.Self)
                ),
            )
        description = "Whenever you cast an instant or sorcery spell, put a charge counter on " +
            "this artifact. Then if there are four or more charge counters on it, you may remove " +
            "those counters and transform it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "243"
        artist = "John Avon"
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4d379b5-7f56-4a7d-a4ac-131fc3d579c6.jpg?1783935709"
        ruling(
            "2017-09-29",
            "Primal Amulet's last ability triggers once you've completed casting a spell. " +
                "Notably, you can't use Primal Wellspring to pay for the spell that gives Primal " +
                "Amulet its fourth counter.",
        )
        ruling(
            "2017-09-29",
            "If a fourth charge counter is put on Primal Amulet by something other than the " +
                "resolution of its ability (as modified by any applicable replacement effects), " +
                "you won't be able to remove those counters and transform it yet. You'll have to " +
                "wait until you cast an instant or sorcery spell again.",
        )
    }
}

private val PrimalWellspring = card("Primal Wellspring") {
    manaCost = ""
    typeLine = "Land"
    oracleText = "(Transforms from Primal Amulet.)\n" +
        "{T}: Add one mana of any color. When that mana is spent to cast an instant or sorcery " +
        "spell, copy that spell and you may choose new targets for the copy."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChoice(
            colorSet = ManaColorSet.AnyColor,
            riders = setOf(
                ManaSpellRider.TriggersWhenSpent(
                    GameObjectFilter.InstantOrSorcery,
                    Effects.CopyTargetSpell(EffectTarget.TriggeringEntity),
                )
            ),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add one mana of any color. When that mana is spent to cast an instant " +
            "or sorcery spell, copy that spell and you may choose new targets for the copy."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "243"
        artist = "John Avon"
        imageUri = "https://cards.scryfall.io/normal/back/d/4/d4d379b5-7f56-4a7d-a4ac-131fc3d579c6.jpg?1783935709"
        ruling(
            "2017-09-29",
            "The mana produced by Primal Wellspring can be spent on anything, not just an " +
                "instant or sorcery spell.",
        )
        ruling(
            "2017-09-29",
            "If more than one mana produced by a Primal Wellspring is spent to cast a single " +
                "instant or sorcery spell, the delayed triggered ability associated with each " +
                "mana spent will trigger. That many copies will be created.",
        )
        ruling(
            "2017-09-29",
            "Primal Wellspring's delayed triggered ability can copy the spell even if that spell " +
                "is countered before the ability resolves.",
        )
    }
}

val PrimalAmulet: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = PrimalAmuletFront,
    backFace = PrimalWellspring,
)
