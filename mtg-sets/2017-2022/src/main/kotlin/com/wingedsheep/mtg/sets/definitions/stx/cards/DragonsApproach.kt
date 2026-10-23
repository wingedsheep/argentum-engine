package com.wingedsheep.mtg.sets.definitions.stx.cards

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
 * Dragon's Approach — Strixhaven: School of Mages #97 (canonical printing)
 * {2}{R} · Sorcery
 *
 * Dragon's Approach deals 3 damage to each opponent. You may exile this spell and four cards
 * named Dragon's Approach from your graveyard. If you do, search your library for a Dragon
 * creature card, put it onto the battlefield, then shuffle.
 * A deck can have any number of cards named Dragon's Approach.
 *
 * The same shape as Sphinx's Approach: an all-or-nothing `Effects.May` over `Effects.IfYouDo`
 * whose success bar is four graveyard copies actually exiled; the resolving spell (or copy, per
 * the ruling) is exiled off the stack via `CardSource.Entity(EffectTarget.Self)`. The
 * deck-construction clause is read from the oracle text by the game server's `DeckValidator`.
 */
val DragonsApproach = card("Dragon's Approach") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Dragon's Approach deals 3 damage to each opponent. You may exile this spell and " +
        "four cards named Dragon's Approach from your graveyard. If you do, search your library for " +
        "a Dragon creature card, put it onto the battlefield, then shuffle.\n" +
        "A deck can have any number of cards named Dragon's Approach."

    spell {
        effect = Effects.DealDamage(3, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.May(
                effect = Effects.IfYouDo(
                    action = Effects.Pipeline {
                        val approaches = gather(
                            CardSource.FromZone(
                                Zone.GRAVEYARD,
                                Player.You,
                                GameObjectFilter.Any.named("Dragon's Approach")
                            )
                        )
                        val four = chooseExactly(
                            count = 4,
                            from = approaches,
                            prompt = "Exile four cards named Dragon's Approach from your graveyard",
                            name = "dragonsApproachExiled"
                        )
                        exile(four)
                        exile(gather(CardSource.Entity(EffectTarget.Self)))
                    },
                    then = Patterns.Library.searchLibrary(
                        filter = GameObjectFilter.Creature.withSubtype("Dragon"),
                        destination = SearchDestination.BATTLEFIELD
                    ),
                    successCriterion = SuccessCriterion.CollectionNonEmpty("dragonsApproachExiled", min = 4),
                ),
                descriptionOverride = "You may exile this spell and four cards named Dragon's Approach " +
                    "from your graveyard. If you do, search your library for a Dragon creature card, " +
                    "put it onto the battlefield, then shuffle.",
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "97"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0cb504a0-1dfb-49d0-84c3-7bd318d55481.jpg?1783927356"
        ruling("2021-04-16", "In most cases, you'll need to exile five cards named Dragon's Approach (the resolving one from the stack plus four more from your graveyard) to search for a Dragon card. However, if Dragon's Approach is a copy, you can still exile it along with four cards named Dragon's Approach from your graveyard to search for a Dragon card. The exiled copy will then cease to exist.")
        ruling("2021-04-16", "The last ability of Dragon's Approach lets you ignore the \"four-of\" rule. It doesn't let you ignore format legality. For example, during a Strixhaven Limited event, you can't add Dragon's Approach from your personal collection.")
    }
}
