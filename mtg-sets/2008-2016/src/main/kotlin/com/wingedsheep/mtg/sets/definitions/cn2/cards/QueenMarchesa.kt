package com.wingedsheep.mtg.sets.definitions.cn2.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Queen Marchesa
 * {1}{R}{W}{B}
 * Legendary Creature — Human Assassin
 * 3/3
 * Deathtouch, haste
 * When Queen Marchesa enters, you become the monarch.
 * At the beginning of your upkeep, if an opponent is the monarch, create a 1/1 black Assassin
 * creature token with deathtouch and haste.
 *
 * "An opponent is the monarch" is an intervening-if (CR 603.4), checked as the upkeep begins and
 * again on resolution — and it is existential over every opponent, not just the first.
 */
val QueenMarchesa = card("Queen Marchesa") {
    manaCost = "{1}{R}{W}{B}"
    colorIdentity = "BRW"
    typeLine = "Legendary Creature — Human Assassin"
    power = 3
    toughness = 3
    oracleText = "Deathtouch, haste\n" +
        "When Queen Marchesa enters, you become the monarch.\n" +
        "At the beginning of your upkeep, if an opponent is the monarch, create a 1/1 black " +
        "Assassin creature token with deathtouch and haste."

    keywords(Keyword.DEATHTOUCH, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.BecomeMonarch()
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.AnOpponentIsTheMonarch
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Assassin"),
            keywords = setOf(Keyword.DEATHTOUCH, Keyword.HASTE)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "78"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/0/3/03f4341c-088b-4f35-b82b-3d98d8a93de4.jpg?1783937339"

        ruling("2016-08-23", "The last ability of Queen Marchesa checks to see if an opponent is the monarch as your upkeep begins. If no opponent is the monarch, Queen Marchesa's ability won't trigger at all. Queen Marchesa's ability will also check to see if an opponent is the monarch as it tries to resolve. If no opponent is the monarch at that time, Queen Marchesa's ability will have no effect.")
        ruling("2016-08-23", "The game starts with no monarch. Once an effect makes one player the monarch, the game will have exactly one monarch from that point forward.")
        ruling("2016-08-23", "Being the monarch carries two inherent triggered abilities. \"At the beginning of the monarch's end step, that player draws a card\" and \"Whenever a creature deals combat damage to the monarch, its controller becomes the monarch.\"")
        ruling("2016-08-23", "Abilities that trigger whenever you \"become the monarch\" trigger only if you aren't already the monarch. For example, if you are already the monarch as Custodi Lich enters the battlefield, its last ability won't trigger.")
        ruling("2016-08-23", "If the triggered ability that causes the monarch to draw a card goes on the stack, and a different player becomes the monarch before that ability resolves, the first player will still draw the card.")
    }
}
