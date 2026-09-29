package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Djeru and Hazoret
 * {2}{R}{R}{W}
 * Legendary Creature — Human God
 * 5/4
 *
 * As long as you have one or fewer cards in hand, Djeru and Hazoret has vigilance and haste.
 * Whenever Djeru and Hazoret attacks, look at the top six cards of your library. You may exile a
 * legendary creature card from among them. Put the rest on the bottom of your library in a random
 * order. Until end of turn, you may cast the exiled card without paying its mana cost.
 *
 * The attack trigger is a look-and-split: up to one legendary creature card is exiled, the rest go
 * to the bottom in a random order, and the exiled card gets an end-of-turn free-cast permission
 * (may-play-from-exile + without-paying-its-mana-cost).
 */
val DjeruAndHazoret = card("Djeru and Hazoret") {
    manaCost = "{2}{R}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Human God"
    power = 5
    toughness = 4
    oracleText = "As long as you have one or fewer cards in hand, Djeru and Hazoret has vigilance " +
        "and haste.\n" +
        "Whenever Djeru and Hazoret attacks, look at the top six cards of your library. You may " +
        "exile a legendary creature card from among them. Put the rest on the bottom of your " +
        "library in a random order. Until end of turn, you may cast the exiled card without paying " +
        "its mana cost."

    staticAbility {
        condition = Conditions.CardsInHandAtMost(1)
        ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter.source())
    }
    staticAbility {
        condition = Conditions.CardsInHandAtMost(1)
        ability = GrantKeyword(Keyword.HASTE, GroupFilter.source())
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(6))
            val (exiled, rest) = chooseUpToSplit(
                1,
                from = looked,
                filter = GameObjectFilter.Creature.legendary(),
                selectedLabel = "Exile",
                remainderLabel = "Put on bottom"
            )
            exile(exiled)
            toLibraryBottom(rest, order = CardOrder.Random)
            run(Effects.GrantMayPlayFromExile(exiled))
            run(Effects.GrantPlayWithoutPayingCost(exiled))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "221"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1db1ae7b-ed48-409f-8d50-07c7e8c6c128.jpg?1783916956"
        ruling("2023-04-14", "Once Djeru and Hazoret has legally attacked during the turn it came under your control, causing it to lose haste by adding cards to your hand won't cause it to stop attacking. Similarly, causing it to lose vigilance after it has attacked won't cause it to become tapped.")
        ruling("2023-04-14", "If you cast a spell without paying its mana cost, you can't choose to cast it for any alternative costs. You can, however, pay any additional costs. If the spell has any mandatory additional costs, you must pay those.")
        ruling("2023-04-14", "If the spell has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
    }
}
