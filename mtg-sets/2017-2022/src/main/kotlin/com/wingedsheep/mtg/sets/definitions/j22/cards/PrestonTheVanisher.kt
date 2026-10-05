package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Preston, the Vanisher — Jumpstart 2022 #8
 * {3}{W} · Legendary Creature — Rabbit Wizard · 2/5
 *
 * Whenever another nontoken creature you control enters, if it wasn't cast, create a token that's
 * a copy of that creature, except it's a 0/1 white Illusion.
 * {1}{W}, Sacrifice five Illusions: Exile target nonland permanent.
 *
 * "If it wasn't cast" is an intervening-if on the *entering* creature's cast record
 * ([Conditions.TriggeringEntityWasCast], negated), as on Rapid Augmenter. The copy's "except it's a
 * 0/1 white Illusion" is a set of copy exceptions (CR 707.9b): the stated color and creature type
 * replace the copied ones (CR 205.1a), and base P/T becomes 0/1. The token is a token, so it never
 * re-triggers Preston.
 */
val PrestonTheVanisher = card("Preston, the Vanisher") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Rabbit Wizard"
    power = 2
    toughness = 5
    oracleText = "Whenever another nontoken creature you control enters, if it wasn't cast, create a token " +
        "that's a copy of that creature, except it's a 0/1 white Illusion.\n" +
        "{1}{W}, Sacrifice five Illusions: Exile target nonland permanent."

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.nontoken().youControl()).enters()
        interveningIf = Conditions.Not(Conditions.TriggeringEntityWasCast)
        effect = Effects.CreateTokenCopyOfTarget(
            target = EffectTarget.TriggeringEntity,
            exceptions = CopyExceptions(
                powerOverride = 0,
                toughnessOverride = 1,
                overrideColors = setOf(Color.WHITE),
                overrideSubtypes = setOf(Subtype.ILLUSION)
            )
        )
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{W}"),
            Costs.SacrificeMultiple(5, GameObjectFilter.Permanent.withSubtype(Subtype.ILLUSION))
        )
        val permanent = target(TargetFilter.NonlandPermanent)
        effect = Effects.Exile(permanent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "8"
        artist = "Christina Kraus"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/952cb0b3-a6b7-4279-8833-3d8890b2d005.jpg?1783919194"
    }
}
