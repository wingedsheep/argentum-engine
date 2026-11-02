package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Hatchery Spider
 * {5}{G}{G}
 * Creature — Spider
 * 5/7
 * Reach
 * Undergrowth — When you cast this spell, reveal the top X cards of your library, where X is the
 * number of creature cards in your graveyard. You may put a green permanent card with mana value X
 * or less from among them onto the battlefield. Put the rest on the bottom of your library in a
 * random order.
 *
 * A cast trigger: it resolves while the Spider is still on the stack (and even if the Spider is
 * countered). X is counted as the trigger resolves; nothing in the pipeline changes the graveyard
 * between the reveal and the pick, so both reads of X agree.
 */
val HatcherySpider = card("Hatchery Spider") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spider"
    oracleText = "Reach\nUndergrowth — When you cast this spell, reveal the top X cards of your library, " +
        "where X is the number of creature cards in your graveyard. You may put a green permanent card " +
        "with mana value X or less from among them onto the battlefield. Put the rest on the bottom of " +
        "your library in a random order."
    power = 5
    toughness = 7

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.self.isCast()
        effect = Effects.Pipeline {
            val x = DynamicAmounts.creatureCardsInYourGraveyard()
            val revealed = gather(CardSource.TopOfLibrary(x), revealed = true)
            val (chosen, rest) = chooseUpToSplit(
                count = 1,
                from = revealed,
                filter = GameObjectFilter.Permanent.withColor(Color.GREEN).manaValueAtMostDynamic(x),
                prompt = "You may put a green permanent card with mana value X or less onto the battlefield"
            )
            move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD))
            toLibraryBottom(rest, order = CardOrder.Random)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "132"
        artist = "Livia Prima"
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b841904-202c-4b42-869a-e345112ab1c8.jpg?1783934150"
        ruling("2018-10-05", "Hatchery Spider's ability triggers as you cast it, and that ability resolves before the spell itself. It resolves even if that spell is countered.")
        ruling("2018-10-05", "If a card in your library has {X} in its mana cost, X is considered to be 0.")
        ruling("2018-10-05", "A land card that produces green mana, even a Forest, normally has no color.")
        ruling("2018-10-05", "Creature cards with other types, such as artifact creature cards, count for undergrowth abilities.")
    }
}
