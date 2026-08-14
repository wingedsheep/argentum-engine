package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Beorn's Hospitality
 * {1}{G}
 * Enchantment
 *
 * Landfall — Whenever a land you control enters, put a +1/+1 counter on target creature you control.
 * {5}{G}{G}: This enchantment becomes a Bear creature in addition to its other types and gains
 * "This creature's power and toughness are each equal to the number of lands you control."
 * (This effect doesn't end.)
 *
 * The landfall half is the plain [Triggers.LandYouControlEnters] shape — an enchantment can never be
 * the entering land, so the trigger's OTHER binding costs nothing here.
 *
 * The activated half animates the enchantment *itself* with `Duration.Permanent` ("This effect
 * doesn't end") and, crucially, a **characteristic-defining** P/T rather than a value frozen at
 * resolution: `dynamicPower`/`dynamicToughness` are re-evaluated at Layer 7b on every projection, so
 * playing another land immediately grows the Bear, and a Wasteland shrinks it. The printed
 * `power`/`toughness` are only the rules-text display (rendered as a star P/T). `creatureTypes` adds Bear
 * and CREATURE without touching the existing Enchantment type, matching "in addition to its other
 * types" — the permanent stays an enchantment creature.
 *
 * Activating twice is harmless: the second animation is a second Layer 4/7b effect setting the same
 * types and the same CDA, so the later timestamp simply wins with an identical result.
 */
val BeornsHospitality = card("Beorn's Hospitality") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Landfall — Whenever a land you control enters, put a +1/+1 counter on target " +
        "creature you control.\n" +
        "{5}{G}{G}: This enchantment becomes a Bear creature in addition to its other types and " +
        "gains \"This creature's power and toughness are each equal to the number of lands you " +
        "control.\" (This effect doesn't end.)"

    triggeredAbility {
        trigger = Triggers.LandYouControlEnters
        val creature = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, creature)
        description = "Landfall — Whenever a land you control enters, put a +1/+1 counter on " +
            "target creature you control."
    }

    activatedAbility {
        cost = Costs.Mana("{5}{G}{G}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = DynamicAmount.Fixed(0),
            toughness = DynamicAmount.Fixed(0),
            creatureTypes = setOf("Bear"),
            duration = Duration.Permanent,
            dynamicPower = DynamicAmounts.landsYouControl(),
            dynamicToughness = DynamicAmounts.landsYouControl()
        )
        description = "{5}{G}{G}: This enchantment becomes a Bear creature in addition to its " +
            "other types and gains \"This creature's power and toughness are each equal to the " +
            "number of lands you control.\""
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "120"
        artist = "Harkalé Linaï"
        imageUri = "https://cards.scryfall.io/normal/front/1/5/153ca57e-30f0-4ad7-ae9d-c55cbf0fd4c9.jpg?1785152153"
    }
}
