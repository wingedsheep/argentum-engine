package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ExertAsItAttacks
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry

/**
 * Anep, Vizier of Hazoret — Foundations Jumpstart #46 (canonical printing; its only printing)
 * {2}{R} · Legendary Creature — Jackal Warrior · 4/2
 *
 * Trample
 * You may exert Anep as it attacks. When you do, exile the top two cards of your library. Until the
 * end of your next turn, you may play those cards.
 *
 * The exert is [ExertAsItAttacks]; the "when you do" rider is its linked self-exert trigger, which
 * resolves before blockers are declared. The rider is Reckless Impulse's impulse draw.
 */
val AnepVizierOfHazoret = card("Anep, Vizier of Hazoret") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Jackal Warrior"
    oracleText = "Trample\n" +
        "You may exert Anep as it attacks. When you do, exile the top two cards of your library. " +
        "Until the end of your next turn, you may play those cards. " +
        "(An exerted creature won't untap during your next untap step.)"
    power = 4
    toughness = 2

    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = ExertAsItAttacks()
    }

    triggeredAbility {
        trigger = Triggers.self.exertedAsItAttacks()
        effect = Patterns.Exile.impulse(2, MayPlayExpiry.UntilEndOfNextTurn)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "46"
        artist = "Moopic"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/295658c4-7ab8-473d-abe1-0b6303b6850f.jpg?1783908856"
        ruling("2024-11-08", "You can exert Anep as you declare it as an attacking creature. You can't do so later in combat, and creatures put onto the battlefield attacking can't be exerted. Any abilities that trigger on exerting an attacking creature will resolve before blockers are declared.")
        ruling("2024-11-08", "If an exerted permanent is already untapped during your next untap step (most likely because an effect untapped it), exert's effect preventing it from untapping expires without having done anything.")
        ruling("2024-11-08", "You pay all costs and follow all timing rules for cards played with the permission granted by the ability that triggers when you exert Anep. For example, if the exiled card is a land card, you may play it only during your main phase while the stack is empty.")
    }
}
