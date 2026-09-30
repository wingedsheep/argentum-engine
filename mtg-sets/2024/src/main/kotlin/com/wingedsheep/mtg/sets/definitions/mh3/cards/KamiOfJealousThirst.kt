package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kami of Jealous Thirst
 * {2}{B}
 * Creature — Spirit
 * 1/3
 *
 * Deathtouch
 * {4}{B}: Each opponent loses 2 life and you gain 2 life. This ability costs {4}{B} less to
 * activate if you've drawn three or more cards this turn. Activate only once each turn.
 *
 * The reduction is the whole printed cost, colored pip included, so it rides the pip-wise
 * `costsLessIf` rail rather than the generic-only `genericCostReduction`.
 */
val KamiOfJealousThirst = card("Kami of Jealous Thirst") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Spirit"
    power = 1
    toughness = 3
    oracleText = "Deathtouch\n{4}{B}: Each opponent loses 2 life and you gain 2 life. This ability costs {4}{B} less to activate if you've drawn three or more cards this turn. Activate only once each turn."

    keywords(Keyword.DEATHTOUCH)

    activatedAbility {
        cost = Costs.Mana("{4}{B}")
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(2)
        costsLessIf("{4}{B}", Conditions.YouDrewCardsThisTurn(3))
        restrictions = listOf(ActivationRestriction.OncePerTurn)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "98"
        artist = "Xavier Ribeiro"
        flavorText = "The bustle of life in Towashi opened up new avenues for less benevolent kami."
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a2958c96-97f8-4961-813c-938b83e20d07.jpg?1783911279"
    }
}
