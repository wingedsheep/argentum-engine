package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Hidetsugu and Kairi — March of the Machine #228
 * {2}{U}{U}{B} · Legendary Creature — Ogre Demon Dragon 5/4
 *
 * Enters: Brainstorm. Dies: exile the top card, the targeted opponent loses life equal to that
 * card's mana value (read off the exiled collection), then an instant or sorcery among it may be
 * cast for free during the resolution (up-to-one pick = the "may").
 */
val HidetsuguAndKairi = card("Hidetsugu and Kairi") {
    manaCost = "{2}{U}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Ogre Demon Dragon"
    power = 5
    toughness = 4
    oracleText = "Flying\n" +
        "When Hidetsugu and Kairi enters, draw three cards, then put two cards from your hand on top " +
        "of your library in any order.\n" +
        "When Hidetsugu and Kairi dies, exile the top card of your library. Target opponent loses " +
        "life equal to its mana value. If it's an instant or sorcery card, you may cast it without " +
        "paying its mana cost."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(3) then Effects.Pipeline {
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Any))
            val putBack = chooseExactly(2, hand)
            toLibraryTop(putBack)
        }
        description = "When Hidetsugu and Kairi enters, draw three cards, then put two cards from " +
            "your hand on top of your library in any order."
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            val top = gather(CardSource.TopOfLibrary(DynamicAmounts.fixed(1)))
            exile(top)
            run(Effects.LoseLife(DynamicAmounts.manaValueSumOf(top.key), opponent))
            val spells = filter(top, GameObjectFilter.InstantOrSorcery)
            val toCast = chooseUpTo(
                1,
                from = spells,
                prompt = "You may cast it without paying its mana cost",
            )
            run(Effects.CastFromCollectionWithoutPayingCost(from = toCast))
        }
        description = "When Hidetsugu and Kairi dies, exile the top card of your library. Target " +
            "opponent loses life equal to its mana value. If it's an instant or sorcery card, you may " +
            "cast it without paying its mana cost."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "228"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81039daf-0d54-4474-a833-fa287ec10cf9.jpg?1783916948"
        ruling("2023-04-14", "The two cards you put on top of your library can be from the three you just drew or ones that were already in your hand.")
        ruling("2023-04-14", "The last triggered ability targets the opponent. If that player is an illegal target as the ability tries to resolve, the ability won't resolve and none of its effects will happen. You won't exile a card.")
        ruling("2023-04-14", "You choose whether or not to cast the instant or sorcery card as the last triggered ability resolves. If you do, you do so as part of the resolution of that ability. You can't wait to cast it later in the turn. Timing restrictions based on the card's type are ignored.")
        ruling("2023-04-14", "If the card has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
    }
}
