package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.PlayFromTopWithAlternativeCost
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bolas's Citadel — War of the Spark #79 (canonical printing)
 * {3}{B}{B}{B}
 * Legendary Artifact
 * You may look at the top card of your library any time.
 * You may play lands and cast spells from the top of your library. If you cast a spell this way,
 * pay life equal to its mana value rather than pay its mana cost.
 * {T}, Sacrifice ten nonland permanents: Each opponent loses 10 life.
 *
 * The play permission is the printed form of Gwenom, Remorseless's grant:
 * [PlayFromTopWithAlternativeCost] with the mana cost waived and
 * `PayLifeEqualToManaValueOfSpell` substituted. With no filter it covers lands too, which are
 * played (using a land drop) rather than cast. The sacrifice cost doesn't exclude the Citadel
 * itself — it may be one of the ten (2019-05-03 ruling).
 */
val BolassCitadel = card("Bolas's Citadel") {
    manaCost = "{3}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Artifact"
    oracleText = "You may look at the top card of your library any time.\n" +
        "You may play lands and cast spells from the top of your library. If you cast a spell this way, " +
        "pay life equal to its mana value rather than pay its mana cost.\n" +
        "{T}, Sacrifice ten nonland permanents: Each opponent loses 10 life."

    staticAbility {
        ability = LookAtTopOfLibrary()
    }

    staticAbility {
        ability = PlayFromTopWithAlternativeCost(
            withoutPayingManaCost = true,
            additionalCost = Costs.additional.PayLifeEqualToManaValueOfSpell,
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeMultiple(10, GameObjectFilter.NonlandPermanent))
        effect = Effects.LoseLife(10, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "{T}, Sacrifice ten nonland permanents: Each opponent loses 10 life."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "79"
        artist = "Jonas De Ro"
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d2124603-d20e-40eb-97f0-a66323397ac2.jpg?1783933451"
        ruling(
            "2019-05-03",
            "You must follow the normal timing permissions and restrictions of the cards you play from your library."
        )
        ruling(
            "2019-05-03",
            "You can play a land card from the top of your library only if you have available land plays remaining."
        )
        ruling(
            "2019-05-03",
            "If a spell has {X} in its mana cost, you must choose 0 as the value of X when casting it without " +
                "paying its mana cost."
        )
        ruling(
            "2019-05-03",
            "If you cast a spell for another cost \"rather than pay its mana cost,\" you can't choose to cast it " +
                "for any alternative costs. You can, however, pay additional costs. If the card has any mandatory " +
                "additional costs, such as that of Spark Harvest, those must be paid to cast the card."
        )
        ruling("2019-05-03", "Bolas's Citadel may be one of the permanents you sacrifice to activate its last ability.")
    }
}
