package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Oona, Queen of the Fae
 * {3}{U/B}{U/B}{U/B}
 * Legendary Creature — Faerie Wizard
 * 5/5
 * Flying
 * {X}{U/B}: Choose a color. Target opponent exiles the top X cards of their library. For each card
 * of the chosen color exiled this way, create a 1/1 blue and black Faerie Rogue creature token with
 * flying.
 *
 * The opponent is targeted on activation; the color is chosen on resolution (2008-05-01 ruling), so
 * [Effects.ChooseColorThen] wraps the whole pipeline. The exile is Villainous Wealth's
 * "target opponent exiles the top X" gather; the payoff counts the exiled collection filtered by
 * `withChosenColor()` (read from the resolution's chosen color), Culmination of Studies' shape.
 */
val OonaQueenOfTheFae = card("Oona, Queen of the Fae") {
    manaCost = "{3}{U/B}{U/B}{U/B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Faerie Wizard"
    power = 5
    toughness = 5
    oracleText = "Flying\n" +
        "{X}{U/B}: Choose a color. Target opponent exiles the top X cards of their library. For each card " +
        "of the chosen color exiled this way, create a 1/1 blue and black Faerie Rogue creature token with flying."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{X}{U/B}")
        target(Targets.Opponent)
        effect = Effects.ChooseColorThen(
            then = Effects.Pipeline {
                val exiled = gather(CardSource.TopOfLibrary(DynamicAmounts.xValue(), player = Player.TargetOpponent))
                exile(exiled, Player.TargetOpponent)
                val ofChosenColor = filter(exiled, GameObjectFilter.Any.withChosenColor())
                run(
                    Effects.CreateToken(
                        count = ofChosenColor.count,
                        power = 1,
                        toughness = 1,
                        colors = setOf(Color.BLUE, Color.BLACK),
                        creatureTypes = setOf("Faerie", "Rogue"),
                        keywords = setOf(Keyword.FLYING),
                        imageUri = "https://cards.scryfall.io/normal/front/a/0/a07b4786-1592-42c7-9d3e-d0d66abaed99.jpg?1783942698",
                    )
                )
            },
            prompt = "Choose a color",
        )
        description = "{X}{U/B}: Choose a color. Target opponent exiles the top X cards of their library. " +
            "For each card of the chosen color exiled this way, create a 1/1 blue and black Faerie Rogue " +
            "creature token with flying."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "172"
        artist = "Adam Rex"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/24ae75f6-fb74-422c-93f6-a0be7f919e47.jpg?1783942730"
        ruling("2008-05-01", "You target the opponent when you activate Oona's ability. You don't choose the color until the ability resolves.")
        ruling("2008-05-01", "The tokens enter under your control.")
    }
}
