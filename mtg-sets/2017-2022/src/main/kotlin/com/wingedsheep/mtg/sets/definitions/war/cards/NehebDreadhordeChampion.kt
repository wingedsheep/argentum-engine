package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ManaExpiry
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Neheb, Dreadhorde Champion
 * {2}{R}{R}
 * Legendary Creature — Zombie Minotaur Warrior
 * 5/4
 *
 * Trample
 * Whenever Neheb deals combat damage to a player or planeswalker, you may discard any number of
 * cards. If you do, draw that many cards and add that much {R}. Until end of turn, you don't lose
 * this mana as steps and phases end.
 *
 * The discarded set is stored and both the draw and the mana read its size (per the ruling, the mana
 * equals the cards discarded, not drawn). Discarding zero draws and adds nothing, so the "you may /
 * if you do" collapses into "any number". The mana is [ManaExpiry.KEPT_UNTIL_END_OF_TURN].
 */
val NehebDreadhordeChampion = card("Neheb, Dreadhorde Champion") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Zombie Minotaur Warrior"
    power = 5
    toughness = 4
    oracleText = "Trample\n" +
        "Whenever Neheb deals combat damage to a player or planeswalker, you may discard any number " +
        "of cards. If you do, draw that many cards and add that much {R}. Until end of turn, you " +
        "don't lose this mana as steps and phases end."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayerOrPlaneswalker)
        effect = Effects.Pipeline {
            val discarded = runStoringCollection { Patterns.Hand.discardAnyNumber(storeAs = it) }
            run(Effects.DrawCards(discarded.count))
            run(Effects.AddMana(Color.RED, discarded.count, expiry = ManaExpiry.KEPT_UNTIL_END_OF_TURN))
        }
        description = "Whenever Neheb deals combat damage to a player or planeswalker, you may discard " +
            "any number of cards. If you do, draw that many cards and add that much {R}. Until end of " +
            "turn, you don't lose this mana as steps and phases end."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "140"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/6/0/60c90d5d-7b3b-48d2-85f6-d6a2a452c0e9.jpg?1783933423"
        ruling("2019-05-03", "Other effects may cause you to draw more or fewer cards than the number of cards you discarded. In these cases, the amount of mana you add is equal to the number of cards you discarded, not the number of cards you drew.")
    }
}
