package com.wingedsheep.mtg.sets.definitions.fra.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sphinx's Approach — Reality Fracture #41
 * {1}{U}{U} · Instant
 *
 * Draw two cards. Then you may exile this spell and four cards named Sphinx's Approach from your
 * graveyard. If you do, search your library for a Sphinx creature card, put it onto the
 * battlefield, then shuffle.
 * A deck can have any number of cards named Sphinx's Approach.
 *
 * - The optional payment is all-or-nothing: an `Effects.May` over a `Gate.DoAction` whose success bar
 *   is four graveyard cards actually exiled (`CollectionNonEmpty(min = 4)`). With fewer than four
 *   other copies in the graveyard the option isn't a legal choice (CR 608.2d), so the gated-effect
 *   executor skips the prompt entirely and the spell goes to the graveyard as usual.
 * - "Exile this spell" happens *during* resolution: `CardSource.Entity(EffectTarget.Self)` gathers the resolving spell
 *   off the stack and the pipeline exiles it alongside the four graveyard copies. Once it has left
 *   the stack, the CR 608.2n "put it into its owner's graveyard" step finds nothing to move. The
 *   spell is on the stack, not in the graveyard, so it can never be one of the four.
 * - The deck-construction clause is read from the oracle text by the game server's
 *   `DeckValidator` ("A deck can have any number of cards named …"), like Relentless Rats.
 */
val SphinxsApproach = card("Sphinx's Approach") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Draw two cards. Then you may exile this spell and four cards named Sphinx's Approach " +
        "from your graveyard. If you do, search your library for a Sphinx creature card, put it onto " +
        "the battlefield, then shuffle.\n" +
        "A deck can have any number of cards named Sphinx's Approach."

    spell {
        effect = Effects.DrawCards(2) then
            Effects.May(
                effect = Effects.IfYouDo(
                    action = Effects.Pipeline {
                        val approaches = gather(
                            CardSource.FromZone(
                                Zone.GRAVEYARD,
                                Player.You,
                                GameObjectFilter.Any.named("Sphinx's Approach")
                            )
                        )
                        val four = chooseExactly(
                            count = 4,
                            from = approaches,
                            prompt = "Exile four cards named Sphinx's Approach from your graveyard",
                            name = "sphinxsApproachExiled"
                        )
                        exile(four)
                        exile(gather(CardSource.Entity(EffectTarget.Self)))
                    },
                    then = Patterns.Library.searchLibrary(
                        filter = GameObjectFilter.Creature.withSubtype("Sphinx"),
                        destination = SearchDestination.BATTLEFIELD
                    ),
                    successCriterion = SuccessCriterion.CollectionNonEmpty("sphinxsApproachExiled", min = 4),
                ),
                descriptionOverride = "You may exile this spell and four cards named Sphinx's Approach " +
                    "from your graveyard. If you do, search your library for a Sphinx creature card, " +
                    "put it onto the battlefield, then shuffle.",
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "41"
        artist = "Nathaniel Himawan"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f49be090-c745-40e5-bc1c-605b8d98acdf.jpg?1789644816"
    }
}
