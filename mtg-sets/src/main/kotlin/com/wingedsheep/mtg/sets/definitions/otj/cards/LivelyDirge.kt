package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Lively Dirge
 * {1}{B}
 * Sorcery
 *
 * Spree (Choose one or more additional costs.)
 * + {1} — Search your library for a card, put it into your graveyard, then shuffle.
 * + {2} — Return up to two creature cards with total mana value 4 or less from your
 *         graveyard to the battlefield.
 *
 * Spree is a [ModalEffect] with `minChooseCount = 1`, `chooseCount = modes.size`, per-mode
 * `additionalManaCost`, and `allowRepeat = false` (CR 700.2 / OTJ release notes). Neither
 * mode targets. Modes always resolve in printed order, so a search-then-reanimate cast can
 * tutor a creature into the graveyard and (because mode 1 resolves first) immediately
 * return it with mode 2.
 *
 * Mode 1 is the atomic gather → select-up-to-one → move-to-graveyard search pipeline
 * ([Patterns.Library.searchLibrary] with `SearchDestination.GRAVEYARD`); "search for a card"
 * is unrestricted and may always fail to find (`ChooseUpTo`). Mode 2 is the
 * gather → select-up-to-two (with [SelectionRestriction.TotalManaValueAtMost] 4) → move-to-
 * battlefield reanimation pipeline (see Scout for Survivors).
 */
val LivelyDirge = card("Lively Dirge") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Spree (Choose one or more additional costs.)\n" +
        "+ {1} — Search your library for a card, put it into your graveyard, then shuffle.\n" +
        "+ {2} — Return up to two creature cards with total mana value 4 or less from your " +
        "graveyard to the battlefield."

    spell {
        effect = ModalEffect(
            modes = listOf(
                // + {1} — Search your library for a card, put it into your graveyard, then shuffle.
                Mode(
                    effect = Patterns.Library.searchLibrary(
                        filter = GameObjectFilter.Any,
                        count = 1,
                        destination = SearchDestination.GRAVEYARD,
                        shuffleAfter = true
                    ),
                    description = "+ {1} — Search your library for a card, put it into your " +
                        "graveyard, then shuffle.",
                    additionalManaCost = "{1}"
                ),
                // + {2} — Return up to two creature cards with total MV 4 or less from your
                //         graveyard to the battlefield.
                Mode(
                    effect = Effects.Composite(
                        listOf(
                            GatherCardsEffect(
                                source = CardSource.FromZone(
                                    zone = Zone.GRAVEYARD,
                                    player = Player.You,
                                    filter = GameObjectFilter.Creature
                                ),
                                storeAs = "dirgeGraveyardCreatures"
                            ),
                            SelectFromCollectionEffect(
                                from = "dirgeGraveyardCreatures",
                                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(2)),
                                restrictions = listOf(SelectionRestriction.TotalManaValueAtMost(4)),
                                storeSelected = "dirgeChosen",
                                prompt = "Choose up to two creature cards with total mana value 4 or less",
                                selectedLabel = "Return to the battlefield"
                            ),
                            MoveCollectionEffect(
                                from = "dirgeChosen",
                                destination = CardDestination.ToZone(Zone.BATTLEFIELD, Player.You)
                            )
                        )
                    ),
                    description = "+ {2} — Return up to two creature cards with total mana value " +
                        "4 or less from your graveyard to the battlefield.",
                    additionalManaCost = "{2}"
                )
            ),
            chooseCount = 2,
            minChooseCount = 1
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "93"
        artist = "Warren Mahy"
        flavorText = "\"Now rip your partner from the grave, and form an undead killing wave!\""
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c35a0d3-12f7-46f3-a6b3-02a490d45ca0.jpg?1712860594"
    }
}
