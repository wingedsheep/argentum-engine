package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.bargain
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Kellan's Lightblades
 * {1}{W}
 * Instant
 *
 * Bargain (You may sacrifice an artifact, enchantment, or token as you cast this spell.)
 * Kellan's Lightblades deals 3 damage to target attacking or blocking creature. If this spell was
 * bargained, destroy that creature instead.
 *
 * The spell-rider shape of bargain (CR 702.166c): the fact is read off the spell while it is still
 * on the stack, so the payoff is gated on [Conditions.WasBargained] at resolution.
 *
 * Unlike Candy Grapple's additive "-5/-5 instead", the word "instead" here swaps the whole effect —
 * a bargained Lightblades deals **no** damage at all, it destroys. So this is a true either/or
 * branch ([ConditionalEffect] with an `elseEffect`) rather than a base effect plus a rider. The
 * distinction is observable: no damage means no lifelink, no damage-triggered abilities, and no
 * marked damage for "damage dealt to it this turn" counts.
 *
 * One target requirement shared by both branches, so the creature is chosen once at announcement
 * regardless of which branch resolves — and the restriction ("attacking or blocking") is checked at
 * announcement and again on resolution, so a creature that stops attacking before this resolves
 * makes the spell fizzle.
 */
val KellansLightblades = card("Kellan's Lightblades") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Bargain (You may sacrifice an artifact, enchantment, or token as you cast this " +
        "spell.)\n" +
        "Kellan's Lightblades deals 3 damage to target attacking or blocking creature. If this " +
        "spell was bargained, destroy that creature instead."

    bargain()

    spell {
        val creature = target(
            "target attacking or blocking creature",
            TargetCreature(filter = TargetFilter.AttackingOrBlockingCreature),
        )
        effect = ConditionalEffect(
            condition = Conditions.WasBargained,
            effect = Effects.Destroy(creature),
            elseEffect = Effects.DealDamage(3, creature),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "18"
        artist = "Fajareka Setiawan"
        flavorText = "Kellan struck out on pure instinct, and his conjured blades cleaved through " +
            "the icy guardians as though they were nothing but air."
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0cc727a6-f875-49b1-b7a4-67f22fbc3d50.jpg?1783915131"

        ruling(
            "2023-09-01",
            "You may sacrifice only one artifact, enchantment, or token to pay a spell's bargain cost."
        )
        ruling(
            "2023-09-01",
            "If you copy a bargained spell, the copy is also bargained."
        )
    }
}
