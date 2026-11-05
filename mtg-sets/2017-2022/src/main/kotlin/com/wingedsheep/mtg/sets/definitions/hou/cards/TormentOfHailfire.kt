package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Torment of Hailfire
 * {X}{B}{B}
 * Sorcery
 *
 * Repeat the following process X times. Each opponent loses 3 life unless that player sacrifices
 * a nonland permanent of their choice or discards a card.
 *
 * The repeat is the outer loop: each process asks every opponent in turn order before the next
 * process begins. Inside [Effects.ForEachPlayer] the iterating opponent is the
 * [EffectTarget.Controller], so they make the choice, sacrifice their own permanent or discard
 * their own card, and lose the life. An option they can't afford isn't offered; with neither a
 * nonland permanent nor a card, they lose 3 life.
 */
val TormentOfHailfire = card("Torment of Hailfire") {
    manaCost = "{X}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Repeat the following process X times. Each opponent loses 3 life unless that player " +
        "sacrifices a nonland permanent of their choice or discards a card."

    spell {
        effect = Effects.Repeat(
            amount = DynamicAmounts.xValue(),
            body = Effects.ForEachPlayer(
                Player.EachOpponent,
                Effects.PayOrSuffer(
                    cost = Costs.pay.Choice(
                        listOf(
                            Costs.pay.Sacrifice(filter = GameObjectFilter.NonlandPermanent),
                            Costs.pay.Discard()
                        )
                    ),
                    suffer = Effects.LoseLife(3, target = EffectTarget.Controller),
                    player = EffectTarget.Controller,
                    // Asked of each opponent, so the consequence is in their words.
                    consequenceDescription = "lose 3 life"
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "77"
        artist = "Grzegorz Rutkowski"
        flavorText = "\"Your God-Pharaoh has returned.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f69d77d1-5980-436c-bf48-790939b069aa.jpg?1783936036"
    }
}
