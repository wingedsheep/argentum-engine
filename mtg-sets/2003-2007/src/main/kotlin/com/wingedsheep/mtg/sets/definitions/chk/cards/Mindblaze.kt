package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.namedFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardNamePool
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Mindblaze — Champions of Kamigawa #180
 * {5}{R} · Sorcery
 *
 * Choose a nonland card name and a number greater than 0. Target player reveals their library.
 * If that library contains exactly the chosen number of cards with the chosen name, Mindblaze
 * deals 8 damage to that player. Then that player shuffles.
 *
 * Both choices are made on resolution by the caster *before* the library is revealed:
 * [Effects.ChooseNumberThen] (min 1 — "greater than 0") stamps the number as X, then the
 * pipeline names a card from the nonland pool, reveals the whole library, and compares the
 * count of named cards in it against X. The shuffle happens whether or not damage is dealt.
 * The number's upper bound is a UI cap only — no library realistically holds 99 copies of a
 * nonland card, so a larger number would never match either.
 */
val Mindblaze = card("Mindblaze") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Choose a nonland card name and a number greater than 0. Target player reveals " +
        "their library. If that library contains exactly the chosen number of cards with the " +
        "chosen name, Mindblaze deals 8 damage to that player. Then that player shuffles."

    spell {
        val player = target(Targets.Player)
        effect = Effects.ChooseNumberThen(
            minValue = 1,
            maxValue = 99,
            prompt = "Choose a number greater than 0",
            then = Effects.Pipeline {
                val chosenName = chooseCardName(pool = CardNamePool.NONLAND)
                val library = gather(CardSource.FromZone(Zone.LIBRARY, Player.TargetPlayer))
                reveal(library)
                run(
                    Effects.If(
                        condition = Conditions.CompareAmounts(
                            DynamicAmounts.count(
                                Player.TargetPlayer,
                                Zone.LIBRARY,
                                GameObjectFilter.Any.namedFromVariable(chosenName)
                            ),
                            ComparisonOperator.EQ,
                            DynamicAmounts.xValue()
                        ),
                        then = Effects.DealDamage(8, player),
                    )
                )
                run(Effects.ShuffleLibrary(target = player))
            }
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "180"
        artist = "John Avon"
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59418766-5567-4ec4-af1f-1cb2db2958d0.jpg?1783944297"
    }
}
