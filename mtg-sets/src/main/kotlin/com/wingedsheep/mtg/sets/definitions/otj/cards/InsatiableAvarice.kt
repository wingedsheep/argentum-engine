package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Insatiable Avarice {B}
 * Sorcery
 *
 * Spree (Choose one or more additional costs.)
 * + {2} — Search your library for a card, then shuffle and put that card on top.
 * + {B}{B} — Target player draws three cards and loses 3 life.
 *
 * Spree is modeled as a [ModalEffect] with `minChooseCount = 1`, `chooseCount = modes.size`,
 * and per-mode `additionalManaCost` (CR 702.166 / OTJ release notes).
 *
 * Mode 1 (tutor to top): [Patterns.Library.searchLibrary] with `SearchDestination.TOP_OF_LIBRARY`
 * shuffles first, then places the found card on top — exactly the printed "shuffle and put that
 * card on top" ordering (cf. Sterling Grove).
 *
 * Mode 2 (draw + drain): the chosen target player draws three cards and loses 3 life. Targeting
 * a player lets you point it at yourself or an opponent.
 */
val InsatiableAvarice = card("Insatiable Avarice") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Spree (Choose one or more additional costs.)\n" +
        "+ {2} — Search your library for a card, then shuffle and put that card on top.\n" +
        "+ {B}{B} — Target player draws three cards and loses 3 life."

    spell {
        effect = ModalEffect(
            modes = listOf(
                Mode(
                    effect = Patterns.Library.searchLibrary(
                        filter = GameObjectFilter.Any,
                        count = 1,
                        destination = SearchDestination.TOP_OF_LIBRARY,
                        shuffleAfter = true
                    ),
                    description = "+ {2} — Search your library for a card, then shuffle and put that card on top.",
                    additionalManaCost = "{2}"
                ),
                Mode(
                    effect = Effects.Composite(
                        Effects.DrawCards(3, EffectTarget.ContextTarget(0)),
                        Effects.LoseLife(3, EffectTarget.ContextTarget(0))
                    ),
                    targetRequirements = listOf(Targets.Player),
                    description = "+ {B}{B} — Target player draws three cards and loses 3 life.",
                    additionalManaCost = "{B}{B}"
                )
            ),
            chooseCount = 2,
            minChooseCount = 1
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "91"
        artist = "Scott Murphy"
        flavorText = "Akul didn't know what lay inside Tarnation's great vault, but the mystery fueled his obsession."
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a108b0e4-1b43-4659-9e91-facb0bd57ebb.jpg?1712860590"

        ruling("2024-04-12", "You must choose at least one of the listed modes and pay its associated additional cost in order to cast a spell with spree.")
        ruling("2024-04-12", "You choose the modes as you cast the spell with spree. Once modes are chosen, they can't be changed.")
        ruling("2024-04-12", "You can't choose the same mode more than once.")
        ruling("2024-04-12", "For the first mode, you shuffle your library before putting the found card on top. The card you searched for won't be shuffled into your library.")
    }
}
