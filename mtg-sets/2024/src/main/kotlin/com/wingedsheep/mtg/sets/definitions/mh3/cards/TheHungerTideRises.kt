package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.references.Player

/**
 * The Hunger Tide Rises
 * {2}{G}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)
 * I, II, III — Create a 1/1 black and green Insect creature token.
 * IV — Sacrifice any number of creatures. Search your library and/or graveyard for a creature card
 *      with mana value less than or equal to the number of creatures sacrificed this way and put it
 *      onto the battlefield. If you search your library this way, shuffle.
 *
 * Chapter IV is one resolution-time pipeline:
 *  - "Sacrifice any number of creatures" gathers the creatures you control, lets you choose any
 *    number of them, and sacrifices them with a tracked move, so the count is the number that
 *    *actually* left (frozen with `storeNumber` before anything else changes the board).
 *  - "Your library and/or graveyard" is a choice of which zones to search, and only searching the
 *    library shuffles it. A nested (resolution-time) modal picks library / graveyard / both, and
 *    each mode is the stock `searchMultipleZones` over those zones — which shuffles and emits the
 *    searched-library event only when `LIBRARY` is among them. The search is choose-up-to-one, so
 *    failing to find is legal (and sacrificing nothing still lets you fetch a mana value 0 creature).
 */
val TheHungerTideRises = card("The Hunger Tide Rises") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)\n" +
        "I, II, III — Create a 1/1 black and green Insect creature token.\n" +
        "IV — Sacrifice any number of creatures. Search your library and/or graveyard for a creature " +
        "card with mana value less than or equal to the number of creatures sacrificed this way and " +
        "put it onto the battlefield. If you search your library this way, shuffle."

    for (chapter in 1..3) {
        sagaChapter(chapter) {
            effect = Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.BLACK, Color.GREEN),
                creatureTypes = setOf("Insect"),
                imageUri = "https://cards.scryfall.io/normal/front/a/5/a5bc4cb0-60d4-4c8f-a420-8bfe635154cd.jpg?1783911112"
            )
        }
    }

    sagaChapter(4) {
        effect = Effects.Pipeline {
            val creatures = gather(GameObjectFilter.Creature, player = Player.You)
            val chosen = chooseAnyNumber(
                from = creatures,
                useTargetingUI = true,
                prompt = "Choose any number of creatures to sacrifice"
            )
            val sacrificed = moveTracked(
                chosen,
                CardDestination.ToZone(Zone.GRAVEYARD),
                moveType = MoveType.Sacrifice
            )
            val sacrificedCount = storeNumber(sacrificed.count)
            val creatureCard = GameObjectFilter.Creature.manaValueAtMostDynamic(sacrificedCount.amount)
            fun search(zones: List<Zone>) = Patterns.Library.searchMultipleZones(
                zones = zones,
                filter = creatureCard,
                count = 1,
                destination = SearchDestination.BATTLEFIELD
            )
            run(
                Effects.Modal(
                    modes = listOf(
                        Mode(description = "Search your library", effect = search(listOf(Zone.LIBRARY))),
                        Mode(description = "Search your graveyard", effect = search(listOf(Zone.GRAVEYARD))),
                        Mode(
                            description = "Search your library and graveyard",
                            effect = search(listOf(Zone.LIBRARY, Zone.GRAVEYARD))
                        )
                    ),
                    countsAsModalSpell = false
                )
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "158"
        artist = "Liiga Smilshkalne"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b44f01b-17e8-4d49-8f38-fd1128114b2f.jpg?1783911260"
        ruling("2024-06-07", "If a card in a player's library has {X} in its mana cost, X is 0 when determining its mana value.")
    }
}
