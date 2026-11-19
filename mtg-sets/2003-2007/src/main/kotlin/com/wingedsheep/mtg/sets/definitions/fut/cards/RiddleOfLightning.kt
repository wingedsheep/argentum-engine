package com.wingedsheep.mtg.sets.definitions.fut.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Riddle of Lightning
 * {3}{R}{R}
 * Instant
 *
 * Choose any target. Scry 3, then reveal the top card of your library. Riddle of Lightning deals
 * damage equal to that card's mana value to that permanent or player.
 *
 * The revealed card stays on top of the library (revealing doesn't move it). If the target is
 * illegal on resolution the spell doesn't resolve at all, so there's no scry or reveal.
 */
val RiddleOfLightning = card("Riddle of Lightning") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Choose any target. Scry 3, then reveal the top card of your library. Riddle of Lightning deals damage equal to that card's mana value to that permanent or player."

    spell {
        val t = target(Targets.Any)
        effect = Effects.Scry(3) then Effects.Pipeline {
            val top = gather(CardSource.TopOfLibrary(1, Player.You))
            reveal(top)
            run(Effects.DealDamage(DynamicAmounts.manaValueOf(top), t))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "105"
        artist = "Daren Bader"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/338be9e2-c0b8-4b69-b7b6-6ba9927a3a4d.jpg?1783943104"
        ruling("2021-03-19", "If the chosen target is an illegal target by the time Riddle of Lightning tries to resolve, the spell doesn't resolve. You don't scry 3 or reveal any card.")
        ruling("2021-03-19", "If a card in a player's library has {X} in its mana cost, X is considered to be 0.")
    }
}
