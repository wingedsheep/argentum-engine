package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.notNamedFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Tibalt's Trickery
 * {1}{R}
 * Instant
 * Counter target spell. Choose 1, 2, or 3 at random. Its controller mills that many cards, then
 * exiles cards from the top of their library until they exile a nonland card with a different name
 * than that spell. They may cast that card without paying its mana cost. Then they put the exiled
 * cards on the bottom of their library in a random order.
 *
 * The spell's name and controller are snapshotted *before* the counter, while it is still a spell
 * on the stack: a countered modal double-faced back face is its front face again in the graveyard
 * (ruling), and a spell cast by a non-owner would read back as its owner's. The counter still
 * happens first, so the free spell can't target the countered one. A spell that can't be
 * countered still gets the rest (ruling).
 *
 * "At random" is [Effects.ChooseNumberThen] with `random = true` — an engine draw, not a die roll —
 * stamped as X for the mill. The rest runs as the countered spell's controller ([Effects.ForEachPlayer]
 * over the snapshot), so the walk, the free cast and the random bottom order are theirs. Only the
 * cards still in exile go to the bottom: a cast card is on the stack by then.
 */
val TibaltsTrickery = card("Tibalt's Trickery") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Counter target spell. Choose 1, 2, or 3 at random. Its controller mills that many " +
        "cards, then exiles cards from the top of their library until they exile a nonland card with " +
        "a different name than that spell. They may cast that card without paying its mana cost. Then " +
        "they put the exiled cards on the bottom of their library in a random order."

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.Pipeline {
            val spell = gather(CardSource.ChosenTargets)
            val spellName = storeCardName(spell)
            val itsController = storePlayer(Player.ControllerOf("target spell"))
            run(Effects.CounterSpell())
            run(
                Effects.ChooseNumberThen(
                    then = Effects.ForEachPlayer(
                        itsController.asPlayers,
                        Effects.Pipeline {
                            mill(DynamicAmounts.xValue())
                            val (found, exiled) = gatherUntilMatch(
                                GameObjectFilter.Nonland.notNamedFromVariable(spellName)
                            )
                            exile(exiled)
                            ifNotEmpty(found) {
                                run(Effects.May(Effects.CastFromCollectionWithoutPayingCost(found)))
                            }
                            val stillExiled = filter(exiled, GameObjectFilter.Any.currentlyIn(Zone.EXILE))
                            toLibraryBottom(stillExiled, order = CardOrder.Random)
                        }
                    ),
                    minValue = 1,
                    maxValue = 3,
                    random = true,
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "153"
        artist = "Anna Podedworna"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd921e27-3e08-438c-bec2-723226d35175.jpg?1783928223"
        ruling(
            "2021-02-05",
            "If there aren't any nonland cards in that library with a different name than the countered " +
                "spell, the player will exile all cards from their library, revealing all of them, and then " +
                "they'll put those cards on the bottom of their library in a random order."
        )
        ruling("2021-02-05", "If the spell has {X} in its mana cost, they must choose 0 as the value of X.")
        ruling(
            "2021-02-05",
            "Tibalt's Trickery can target a spell that can't be countered. If Tibalt's Trickery resolves, " +
                "the target spell won't be countered, but all of the other effects will still happen."
        )
    }
}
