package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder

/**
 * Treasure Keeper — Aether Revolt #177 (canonical printing)
 * {4} · Artifact Creature — Construct · 3/3 · Uncommon
 *
 * When this creature dies, reveal cards from the top of your library until you reveal a nonland
 * card with mana value 3 or less. You may cast that card without paying its mana cost. Put all
 * revealed cards not cast this way on the bottom of your library in a random order.
 *
 * The same reveal-until / may-cast-free / bottom-the-rest pipeline as Plargg, Dean of Chaos's
 * activated ability, minus the nonlegendary restriction: the revealed cards stay in the library
 * (cast from there, as Sunbird's Invocation does), and only after the cast is offered is every
 * revealed card still in the library — including a found card that wasn't cast — put on the
 * bottom in a random order (ruling).
 */
val TreasureKeeper = card("Treasure Keeper") {
    manaCost = "{4}"
    typeLine = "Artifact Creature — Construct"
    power = 3
    toughness = 3
    oracleText = "When this creature dies, reveal cards from the top of your library until you reveal a " +
        "nonland card with mana value 3 or less. You may cast that card without paying its mana cost. " +
        "Put all revealed cards not cast this way on the bottom of your library in a random order."

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.Pipeline {
            val (found, revealed) = gatherUntilMatch(GameObjectFilter.Nonland.manaValueAtMost(3))
            reveal(revealed)
            ifNotEmpty(found) {
                run(Effects.May(Effects.CastFromCollectionWithoutPayingCost(found)))
            }
            val notCast = filter(revealed, GameObjectFilter.Any.currentlyIn(Zone.LIBRARY))
            toLibraryBottom(notCast, order = CardOrder.Random)
        }
        description = "When this creature dies, reveal cards from the top of your library until you " +
            "reveal a nonland card with mana value 3 or less. You may cast that card without paying its " +
            "mana cost. Put all revealed cards not cast this way on the bottom of your library in a " +
            "random order."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "177"
        artist = "James Paick"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c5d73d2-60cc-4b64-8274-4ba1bb98b6fa.jpg?1783936719"
        ruling("2020-08-07", "If you don't cast the nonland card with mana value 3 or less, it'll be put on the bottom of your library in a random order with the other cards.")
        ruling("2020-08-07", "If your library has no nonland cards in it with mana value 3 or less, you'll reveal all the cards in your library, then put them back in a random order.")
        ruling("2020-08-07", "If a card in a player's library has {X} in its mana cost, X is considered to be 0.")
        ruling("2020-08-07", "If you cast a spell \"without paying its mana cost,\" you can't choose to cast it for any alternative costs. You can, however, pay additional costs. If the card has any mandatory additional costs, those must be paid to cast the spell.")
    }
}
