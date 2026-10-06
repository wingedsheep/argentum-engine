package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Band Together
 * {2}{G}
 * Instant
 * Up to two target creatures you control each deal damage equal to their power to another target
 * creature.
 *
 * Same shape as Tandem Takedown: the victim may be a creature you control, so each dealer slot gets
 * its own handle. The victim is declared first because the dealer requirement is variable-width
 * (0–2 targets) and alignment is positional; the dealer requirement is a [TargetOther] so neither
 * dealer can also be the victim ("another target"). An unchosen or illegal dealer slot resolves to
 * nothing, so the other dealer still deals damage (ruling); an illegal victim means nothing is dealt
 * damage.
 */
val BandTogether = card("Band Together") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Up to two target creatures you control each deal damage equal to their power to " +
        "another target creature."

    spell {
        val victim = target(TargetFilter.Creature)
        val (first, second) = targets(
            TargetOther(TargetObject(filter = TargetFilter.CreatureYouControl, count = 2, optional = true))
        )
        effect = Effects.DealDamage(DynamicAmounts.powerOf(first), victim, damageSource = first) then
            Effects.DealDamage(DynamicAmounts.powerOf(second), victim, damageSource = second)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "153"
        artist = "Josh Hass"
        flavorText = "In times of peril, the vision the ancient paruns had for their city comes into focus."
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d1d8aa1-d742-477c-819a-0113912d5011.jpg?1783933416"
        ruling(
            "2019-05-03",
            "If one of the two target creatures you control is an illegal target as Band Together " +
                "resolves, the other will still deal damage equal to its power."
        )
        ruling(
            "2019-05-03",
            "If the last target creature is an illegal target as Band Together resolves, or if both " +
                "of the first targets are illegal targets, no creature deals or is dealt damage."
        )
    }
}
