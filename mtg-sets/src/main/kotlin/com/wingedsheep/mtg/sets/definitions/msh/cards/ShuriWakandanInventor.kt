package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Shuri, Wakandan Inventor — Marvel Super Heroes #75 (uncommon)
 * {1}{U} · Legendary Creature — Human Artificer Hero · 2/1
 *
 * Artifact spells you cast cost {1} less to cast.
 * {1}, {T}: Target artifact you control becomes a copy of a second target artifact you control
 * until end of turn, except it isn't legendary. Activate only as a sorcery.
 *
 * The discount is the Baron Strucker shape: a [ModifySpellCost] static over
 * [SpellCostTarget.YouCast] reducing generic mana by one.
 *
 * The copy is the Fleeting Reflection shape —
 * [Effects.EachPermanentBecomesCopyOfTarget] with `affected` (the artifact that changes identity)
 * separate from `target` (the artifact whose characteristics are copied), the second wrapped in
 * [TargetOther] so "a **second** target" can't be the first one again (CR 601.2c only forbids
 * repeats within one instance of the word "target").
 *
 * **"except it isn't legendary" is the load-bearing clause.** It's a CR 707.9b characteristic
 * modification in the *removal* direction: without it a copy of a legendary artifact would be a
 * second legendary permanent with the same name under one controller, and the legend rule
 * (CR 704.5j) would bin one of them as a state-based action before the copy did anything. Modelled
 * as [CopyExceptions.removedSupertypes] — the same field the token-copy path already had, now
 * shared by both copy paths.
 */
val ShuriWakandanInventor = card("Shuri, Wakandan Inventor") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Artificer Hero"
    power = 2
    toughness = 1
    oracleText = "Artifact spells you cast cost {1} less to cast.\n" +
        "{1}, {T}: Target artifact you control becomes a copy of a second target artifact you " +
        "control until end of turn, except it isn't legendary. Activate only as a sorcery."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Artifact),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        val becomesCopy = target(
            "target artifact you control",
            TargetObject(filter = TargetFilter(GameObjectFilter.Artifact.youControl())),
        )
        val copySource = target(
            "a second target artifact you control",
            TargetOther(
                baseRequirement = TargetObject(
                    filter = TargetFilter(GameObjectFilter.Artifact.youControl()),
                ),
            ),
        )
        effect = Effects.EachPermanentBecomesCopyOfTarget(
            target = copySource,
            affected = becomesCopy,
            duration = Duration.EndOfTurn,
            exceptions = CopyExceptions(removedSupertypes = setOf(Supertype.LEGENDARY)),
        )
        timing = TimingRule.SorcerySpeed
        description = "{1}, {T}: Target artifact you control becomes a copy of a second target " +
            "artifact you control until end of turn, except it isn't legendary. Activate only as " +
            "a sorcery."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "75"
        artist = "Wayne Wu"
        flavorText = "While her older brother T'Challa ruled Wakanda, Shuri perfected its defenses " +
            "and vibranium-based weapons."
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65005522-555e-4479-939c-be16e0262f6f.jpg?1783902952"
    }
}
