package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Beseech the Queen
 * {2/B}{2/B}{2/B}
 * Sorcery
 *
 * ({2/B} can be paid with any two mana or with {B}. This card's mana value is 6.)
 * Search your library for a card with mana value less than or equal to the number of lands you
 * control, reveal it, put it into your hand, then shuffle.
 *
 * - Monocoloured hybrid goes into `manaCost` verbatim (Flame Javelin); the parser derives mana
 *   value 6.
 * - The cap is a `manaValueAtMostDynamic` over the count of lands you control, read when the
 *   search resolves.
 */
val BeseechTheQueen = card("Beseech the Queen") {
    manaCost = "{2/B}{2/B}{2/B}"
    typeLine = "Sorcery"
    oracleText = "({2/B} can be paid with any two mana or with {B}. This card's mana value is 6.)\n" +
        "Search your library for a card with mana value less than or equal to the number of lands " +
        "you control, reveal it, put it into your hand, then shuffle."

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Any.manaValueAtMostDynamic(
                DynamicAmounts.battlefield(Player.You, GameObjectFilter.Land).count()
            ),
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true,
            shuffleAfter = true,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "57"
        artist = "Jason Chan"
        flavorText = "Those who hear her go mad with inspiration."
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64ee0a93-0f6d-42be-bdca-1de5422d8d54.jpg?1783942757"
        ruling("2008-05-01", "A card with monocolored hybrid mana symbols in its mana cost has a mana value equal to the highest possible cost it could be cast for. Its mana value never changes. Thus, Beseech the Queen has a mana value of 6, even if you spend {B}{B}{B} to cast it.")
        ruling("2008-05-01", "If a cost includes more than one monocolored hybrid mana symbol, you can choose a different way to pay for each symbol. For example, you can pay for Beseech the Queen by spending {B}{B}{B}, {2}{B}{B}, {4}{B}, or {6}.")
    }
}
