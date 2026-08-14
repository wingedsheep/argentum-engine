package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Velvetwing Butterflies // Gaze in Wonder
 * {2}{W}
 * Creature — Insect
 * 2/2
 *
 * Flying
 *
 * Adventure: Gaze in Wonder — {1}{W}, Instant — Adventure
 * Tap one or two target creatures.
 *
 * "One or two target creatures" is `TargetCreature(count = 2, minCount = 1)` (Succumb to the Cold);
 * the tap runs per chosen target through [ForEachTargetEffect], so a target that has become illegal
 * by resolution is skipped instead of fizzling the whole spell.
 *
 * (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the caster
 * cast it as the creature spell while it remains in exile.)
 */
val VelvetwingButterflies = card("Velvetwing Butterflies") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Insect"
    power = 2
    toughness = 2
    oracleText = "Flying"

    keywords(Keyword.FLYING)

    adventure("Gaze in Wonder") {
        manaCost = "{1}{W}"
        typeLine = "Instant — Adventure"
        oracleText = "Tap one or two target creatures. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            target = TargetCreature(count = 2, minCount = 1, filter = TargetFilter.Creature)
            effect = ForEachTargetEffect(listOf(Effects.Tap(EffectTarget.ContextTarget(0))))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "30"
        artist = "Xabi Gaztelua"
        flavorText = "Bilbo's eyes were nearly blinded by the light. He could only hold on and blink " +
            "at the sight of them."
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5cc0f994-5048-4898-926e-b56cbc97e0ca.jpg?1785497031"
    }
}
