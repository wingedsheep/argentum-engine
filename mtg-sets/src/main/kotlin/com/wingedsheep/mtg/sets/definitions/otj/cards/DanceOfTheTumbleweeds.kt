package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Dance of the Tumbleweeds {1}{G}
 * Sorcery
 *
 * Spree (Choose one or more additional costs.)
 * + {1} — Search your library for a basic land card or a Desert card, put it onto the battlefield, then shuffle.
 * + {3} — Create an X/X green Elemental creature token, where X is the number of lands you control.
 *
 * Spree is modeled as a [ModalEffect] with `minChooseCount = 1`, `chooseCount = modes.size`,
 * and per-mode `additionalManaCost` (CR 702.166): at least one mode must be chosen, and no
 * mode can be chosen more than once. The Elemental token's P/T is a dynamic count of lands
 * you control at resolution ([DynamicAmount.AggregateBattlefield] over [GameObjectFilter.Land]).
 */
val DanceOfTheTumbleweeds = card("Dance of the Tumbleweeds") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Spree (Choose one or more additional costs.)\n" +
        "+ {1} — Search your library for a basic land card or a Desert card, put it onto the battlefield, then shuffle.\n" +
        "+ {3} — Create an X/X green Elemental creature token, where X is the number of lands you control."

    spell {
        effect = ModalEffect(
            modes = listOf(
                Mode(
                    effect = Patterns.Library.searchLibrary(
                        filter = GameObjectFilter.BasicLand or
                            GameObjectFilter.Land.withSubtype(Subtype.DESERT),
                        count = 1,
                        destination = SearchDestination.BATTLEFIELD
                    ),
                    description = "+ {1} — Search your library for a basic land card or a Desert " +
                        "card, put it onto the battlefield, then shuffle.",
                    additionalManaCost = "{1}"
                ),
                Mode(
                    effect = Effects.CreateDynamicToken(
                        dynamicPower = DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Land),
                        dynamicToughness = DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Land),
                        colors = setOf(Color.GREEN),
                        creatureTypes = setOf("Elemental"),
                        imageUri = "https://cards.scryfall.io/normal/front/0/0/008695e6-6d6f-4c16-bf05-377e8cc5f5ff.jpg?1712316611"
                    ),
                    description = "+ {3} — Create an X/X green Elemental creature token, where X is " +
                        "the number of lands you control.",
                    additionalManaCost = "{3}"
                )
            ),
            chooseCount = 2,
            minChooseCount = 1
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "160"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/caf0e715-befb-4904-82e6-d3f8c7fbd454.jpg?1712860584"

        ruling("2024-04-12", "You must choose at least one of the listed modes and pay its associated additional cost in order to cast a spell with spree.")
        ruling("2024-04-12", "You choose the modes as you cast the spell with spree. Once modes are chosen, they can't be changed.")
        ruling("2024-04-12", "You can't choose the same mode more than once.")
        ruling("2024-04-12", "The Elemental's power and toughness are each equal to the number of lands you control as the token's creation resolves.")
    }
}
