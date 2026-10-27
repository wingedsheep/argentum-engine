package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Tuskeri Firewalker — Kaldheim #157 (canonical printing)
 * {2}{R} · Creature — Human Berserker · 3/2
 *
 * Boast — {1}: Exile the top card of your library. You may play that card this turn.
 *
 * The boast body is plain impulse draw ([Patterns.Exile.impulse]) with the default end-of-turn
 * expiry: an unplayed card stays exiled, and a later boast never re-grants it.
 */
val TuskeriFirewalker = card("Tuskeri Firewalker") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Berserker"
    power = 3
    toughness = 2
    oracleText = "Boast — {1}: Exile the top card of your library. You may play that card this turn. " +
        "(Activate only if this creature attacked this turn and only once each turn.)"

    activatedAbility {
        isBoast = true
        cost = Costs.Mana("{1}")
        effect = Patterns.Exile.impulse(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "157"
        artist = "Victor Adame Minguez"
        flavorText = "\"Toralf blessed me with fire. I am worthy of Starnheim!\""
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a54d0170-a375-4e65-b98d-3e94a3aeef90.jpg?1783928221"
        ruling("2021-02-05", "The card you exile with the boast ability is exiled face up.")
        ruling("2021-02-05", "Playing a card this way follows the normal rules for playing the card. You must pay its costs, and you must follow all applicable timing rules. For example, if the card is a sorcery card, you can cast it only during your main phase while the stack is empty.")
        ruling("2021-02-05", "Under normal circumstances, you can play a land this way only if you haven't played a land yet that turn.")
        ruling("2021-02-05", "If you don't play the card by the end of the turn, it will remain exiled. Boasting again on a future turn won't let you play that card.")
        ruling("2021-02-05", "A boast ability can be activated at any point after the creature with that ability has been declared as an attacker. This can be before blockers are declared, after blockers are declared but before combat damage is dealt, during combat after combat damage is dealt, during the postcombat main phase, during the end step, or, in some unusual cases, during the cleanup step.")
        ruling("2021-02-05", "If it's not your turn and you gain control of a creature with a boast ability after that creature attacked, you can activate that creature's boast ability if it hasn't been activated yet that turn.")
        ruling("2021-02-05", "If a creature with a boast ability is put onto the battlefield attacking, it was never declared as an attacker. Its boast ability can't be activated that turn.")
        ruling("2021-02-05", "If an effect adds additional combat phases to a turn and a creature with a boast ability attacks more than once during that turn, its boast ability can still be activated only once.")
    }
}
