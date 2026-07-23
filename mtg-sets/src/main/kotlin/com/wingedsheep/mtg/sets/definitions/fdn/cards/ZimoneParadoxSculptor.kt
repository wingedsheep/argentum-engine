package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Zimone, Paradox Sculptor
 * {2}{G}{U}
 * Legendary Creature — Human Wizard
 * 1/4
 *
 * At the beginning of combat on your turn, put a +1/+1 counter on each of up to two target
 * creatures you control.
 * {G}{U}, {T}: Double the number of each kind of counter on up to two target creatures
 * and/or artifacts you control.
 *
 * Both abilities use the "each of up to two targets" shape — an `optional` requirement with
 * `count = 2` fanned out by [ForEachTargetEffect], so choosing zero, one, or two targets all
 * work and each chosen permanent is processed independently. The activated ability doubles
 * *every* kind of counter via [Effects.DoubleAllCounters]; each kind is placed separately, so
 * counter-placement replacements (Hardened Scales, Branching Evolution) apply per kind, per
 * the printed ruling. Artifacts are legal targets alongside creatures, hence the wider
 * [TargetFilter.CreatureOrArtifact].
 */
val ZimoneParadoxSculptor = card("Zimone, Paradox Sculptor") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "UG"
    typeLine = "Legendary Creature — Human Wizard"
    power = 1
    toughness = 4
    oracleText = "At the beginning of combat on your turn, put a +1/+1 counter on each of up to " +
        "two target creatures you control.\n" +
        "{G}{U}, {T}: Double the number of each kind of counter on up to two target creatures " +
        "and/or artifacts you control."

    triggeredAbility {
        trigger = Triggers.BeginCombat
        target(
            "up to two target creatures you control",
            TargetCreature(count = 2, optional = true, filter = TargetFilter.Creature.youControl())
        )
        effect = ForEachTargetEffect(
            listOf(Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0)))
        )
        description = "At the beginning of combat on your turn, put a +1/+1 counter on each of " +
            "up to two target creatures you control."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}{U}"), Costs.Tap)
        target(
            "up to two target creatures and/or artifacts you control",
            TargetObject(
                count = 2,
                optional = true,
                filter = TargetFilter.CreatureOrArtifact.youControl()
            )
        )
        effect = ForEachTargetEffect(
            listOf(Effects.DoubleAllCounters(EffectTarget.ContextTarget(0)))
        )
        description = "Double the number of each kind of counter on up to two target creatures " +
            "and/or artifacts you control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "126"
        artist = "Nathaniel Himawan"
        flavorText = "\"Extraplanar geometry isn't so hard once you get used to thinking in twelve dimensions.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/0/20ccbfdd-ddae-440c-9bc0-38b15a56fdd1.jpg?1783909090"
        ruling(
            "2024-11-08",
            "To double the number of each kind of counter on a permanent, put another counter on " +
                "it for each counter it already has. Effects that interact with counters being put " +
                "onto permanents, such as the effect of Branching Evolution, apply as appropriate.",
        )
    }
}
