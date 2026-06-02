package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Spirit of Resistance
 * {2}{W}
 * Enchantment
 * As long as you control a permanent of each color, prevent all damage that would be dealt to you.
 *
 * Invasion engine gap #7 / #2: the "as long as …" gate uses the new [PreventDamage.restrictions]
 * list (mirroring `ModifyLifeLoss.restrictions`) rather than a bespoke conditional-replacement
 * wrapper. The five-color condition is a distinct-colors aggregation capped at 5 (a single
 * five-color permanent satisfies it), evaluated against the source's controller each time damage
 * would be dealt.
 */
val SpiritOfResistance = card("Spirit of Resistance") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "As long as you control a permanent of each color, prevent all damage that would be dealt to you."

    replacementEffect(
        PreventDamage(
            restrictions = listOf(
                Compare(
                    DynamicAmounts.colorsAmongPermanents(Player.You),
                    ComparisonOperator.GTE,
                    DynamicAmount.Fixed(5)
                )
            ),
            appliesTo = EventPattern.DamageEvent(recipient = RecipientFilter.You)
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "38"
        artist = "John Avon"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5fb66439-df73-4a01-a8d4-6f2334297fdf.jpg?1562914374"
    }
}
