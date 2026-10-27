package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Varragoth, Bloodsky Sire — Kaldheim #115 (canonical printing)
 * {2}{B} · Legendary Creature — Demon Rogue · 2/3
 *
 * Deathtouch
 * Boast — {1}{B}: Target player searches their library for a card, then shuffles and puts that
 * card on top.
 *
 * The search runs inside `ForEachPlayer` over the targeted player, so that player searches, chooses
 * the card, and shuffles their own library before the card goes on top — the same shape Fertilid
 * uses for "target player searches their library".
 */
val VarragothBloodskySire = card("Varragoth, Bloodsky Sire") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Demon Rogue"
    power = 2
    toughness = 3
    oracleText = "Deathtouch\nBoast — {1}{B}: Target player searches their library for a card, then shuffles " +
        "and puts that card on top. (Activate only if this creature attacked this turn and only once each turn.)"

    keywords(Keyword.DEATHTOUCH)

    activatedAbility {
        isBoast = true
        cost = Costs.Mana("{1}{B}")
        val player = target(Targets.Player)
        effect = Effects.ForEachPlayer(
            player.asPlayer,
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Any,
                destination = SearchDestination.TOP_OF_LIBRARY
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "115"
        artist = "Tyler Jacobson"
        flavorText = "\"I waded through a river of fire to bring death to Bretagard.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/f/efa8dbf0-4e5a-452b-826f-5813e8cd9d85.jpg?1783928237"
        ruling("2021-02-05", "A boast ability can be activated at any point after the creature with that ability has been declared as an attacker. This can be before blockers are declared, after blockers are declared but before combat damage is dealt, during combat after combat damage is dealt, during the postcombat main phase, during the end step, or, in some unusual cases, during the cleanup step.")
        ruling("2021-02-05", "If it's not your turn and you gain control of a creature with a boast ability after that creature attacked, you can activate that creature's boast ability if it hasn't been activated yet that turn.")
        ruling("2021-02-05", "If a creature with a boast ability is put onto the battlefield attacking, it was never declared as an attacker. Its boast ability can't be activated that turn.")
        ruling("2021-02-05", "If an effect adds additional combat phases to a turn and a creature with a boast ability attacks more than once during that turn, its boast ability can still be activated only once.")
    }
}
