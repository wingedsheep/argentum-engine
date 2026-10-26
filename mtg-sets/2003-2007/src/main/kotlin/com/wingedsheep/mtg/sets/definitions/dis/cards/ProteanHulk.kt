package com.wingedsheep.mtg.sets.definitions.dis.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Protean Hulk
 * {5}{G}{G}
 * Creature — Beast
 * 6/6
 *
 * When this creature dies, search your library for any number of creature cards with total mana
 * value 6 or less, put them onto the battlefield, then shuffle.
 *
 * Canonical printing: Dissension, the card's earliest real printing. The search is an any-number
 * pick over the library's creature cards capped by [SelectionRestriction.TotalManaValueAtMost],
 * so picks that would push the total past 6 are dropped ({X} counts as 0 in a library).
 */
val ProteanHulk = card("Protean Hulk") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    power = 6
    toughness = 6
    oracleText = "When this creature dies, search your library for any number of creature cards with total " +
        "mana value 6 or less, put them onto the battlefield, then shuffle."

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Creature),
                search = true,
            )
            val found = chooseAnyNumber(
                from = searchable,
                restrictions = listOf(SelectionRestriction.TotalManaValueAtMost(6)),
                prompt = "Search your library for any number of creature cards with total mana value 6 or less",
                selectedLabel = "Put onto the battlefield",
            )
            move(found, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "90"
        artist = "Matt Cavotta"
        flavorText = "\"Meat *and* eggs. We eat!\"\n—Borborygmos"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d978332-95bf-4f86-9e67-06f10983c267.jpg?1783943412"
        ruling("2018-03-16", "If a card in your library has {X} in its mana cost, X is considered to be 0.")
        ruling(
            "2018-03-16",
            "You can find any number of creature cards, so long as their total mana value is 6 or less. For " +
                "example, you could find eight creature cards with mana value 0 and three with mana value 2, but " +
                "you couldn't find two with mana value 4."
        )
    }
}
