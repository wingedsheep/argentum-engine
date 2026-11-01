package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * Niv-Mizzet Reborn
 * {W}{U}{B}{R}{G}
 * Legendary Creature — Dragon Avatar
 * 6/6
 *
 * Flying
 * When Niv-Mizzet enters, reveal the top ten cards of your library. For each color pair, choose a
 * card that's exactly those colors from among them. Put the chosen cards into your hand and the
 * rest on the bottom of your library in a random order.
 *
 * "For each color pair" is ten mandatory picks of one card each, one per pair, each filtered to
 * "has both colors and exactly two colors" (CR 105.2). A card that is exactly two colors belongs to
 * exactly one pair, so the ten picks are disjoint and their order can't change the outcome; each
 * pick draws from what the previous ones left. A pick with no candidate is skipped, a pick with one
 * candidate takes it without asking, and only a pair with two or more candidates prompts. The cards
 * stay revealed in the library throughout; the chosen ones are "the revealed cards minus what is
 * left", which go to hand before the rest go to the bottom in a random order.
 */
private val colorPairs: List<Pair<Color, Color>> = listOf(
    Color.WHITE to Color.BLUE,
    Color.WHITE to Color.BLACK,
    Color.BLUE to Color.BLACK,
    Color.BLUE to Color.RED,
    Color.BLACK to Color.RED,
    Color.BLACK to Color.GREEN,
    Color.RED to Color.GREEN,
    Color.RED to Color.WHITE,
    Color.GREEN to Color.WHITE,
    Color.GREEN to Color.BLUE,
)

val NivMizzetReborn = card("Niv-Mizzet Reborn") {
    manaCost = "{W}{U}{B}{R}{G}"
    colorIdentity = "WUBRG"
    typeLine = "Legendary Creature — Dragon Avatar"
    power = 6
    toughness = 6
    oracleText = "Flying\n" +
        "When Niv-Mizzet enters, reveal the top ten cards of your library. For each color pair, " +
        "choose a card that's exactly those colors from among them. Put the chosen cards into your " +
        "hand and the rest on the bottom of your library in a random order."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val revealed = gather(CardSource.TopOfLibrary(10), revealed = true)
            var pool = revealed
            for ((first, second) in colorPairs) {
                val (_, rest) = chooseExactlySplit(
                    1,
                    from = pool,
                    filter = GameObjectFilter.Any.withColor(first).withColor(second).exactlyColors(2),
                    prompt = "Choose a ${first.displayName.lowercase()}-${second.displayName.lowercase()} " +
                        "card to put into your hand",
                    selectedLabel = "Put into hand",
                    showAllCards = true
                )
                pool = rest
            }
            val chosen = exclude(revealed, minus = pool)
            move(chosen, CardDestination.ToZone(Zone.HAND))
            move(
                pool,
                CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
                order = CardOrder.Random
            )
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "208"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/5/6/56a2609d-b535-400b-81d9-72989a33c70f.jpg?1783933391"
        ruling("2019-05-03", "A \"color pair\" is exactly two colors. There are ten color pairs in Magic: white-blue, white-black, blue-black, blue-red, black-red, black-green, red-green, red-white, green-white, and green-blue.")
        ruling("2019-05-03", "If the top ten cards of your library don't contain all ten color pairs, you choose as many cards as you can and put those cards into your hand.")
    }
}
