package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate

/**
 * Cleon, Merry Champion — Foundations Jumpstart #47 (canonical printing)
 * {2}{R} · Legendary Creature — Human Soldier · 2/2
 *
 * Double strike
 * Heroic — Whenever you cast a spell that targets Cleon, exile the top card of your library.
 * You may play that card until the end of your next turn.
 *
 * Heroic is the `TargetsSource` cast trigger (Arena Athlete, Akroan Crusader); the payoff is the
 * one-card impulse draw with the turn-keyed `UntilEndOfNextTurn` expiry (Reckless Impulse).
 */
val CleonMerryChampion = card("Cleon, Merry Champion") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "Double strike\n" +
        "Heroic — Whenever you cast a spell that targets Cleon, exile the top card of your library. " +
        "You may play that card until the end of your next turn."

    keywords(Keyword.DOUBLE_STRIKE)

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Patterns.Exile.impulse(1, MayPlayExpiry.UntilEndOfNextTurn)
        description = "Heroic — Whenever you cast a spell that targets Cleon, exile the top card of your " +
            "library. You may play that card until the end of your next turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "47"
        artist = "Mebaru"
        flavorText = "\"Let us dance 'round the funeral pyres of our foes!\""
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e793fae6-ca9a-49e7-b90d-fe7e9c72de0d.jpg?1783908856"
        ruling("2024-11-08", "Cleon's second ability resolves before the spell that caused it to trigger. It resolves even if that spell is countered or otherwise leaves the stack without resolving.")
        ruling("2024-11-08", "You pay all costs and follow all timing rules for cards played with the permission granted by Cleon's last ability. For example, if the exiled card is a land card, you may play it only during your main phase while the stack is empty.")
    }
}
