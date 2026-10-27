package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Horizon Seeker — Kaldheim #175 (canonical printing)
 * {2}{G} · Creature — Human Warrior · 3/2
 *
 * Boast — {1}{G}: Search your library for a basic land card, reveal it, put it into your hand,
 * then shuffle. (Activate only if this creature attacked this turn and only once each turn.)
 */
val HorizonSeeker = card("Horizon Seeker") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Warrior"
    power = 3
    toughness = 2
    oracleText = "Boast — {1}{G}: Search your library for a basic land card, reveal it, put it into " +
        "your hand, then shuffle. (Activate only if this creature attacked this turn and only once each turn.)"

    activatedAbility {
        isBoast = true
        cost = Costs.Mana("{1}{G}")
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            destination = SearchDestination.HAND,
            reveal = true
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "175"
        artist = "Matt Stewart"
        flavorText = "\"A five-day climb? Ha! I'll do it in two.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a17f4b21-6478-407d-b567-2888349f3b66.jpg?1783928213"
        ruling("2021-02-05", "A boast ability can be activated at any point after the creature with that ability has been declared as an attacker. This can be before blockers are declared, after blockers are declared but before combat damage is dealt, during combat after combat damage is dealt, during the postcombat main phase, during the end step, or, in some unusual cases, during the cleanup step.")
        ruling("2021-02-05", "If it's not your turn and you gain control of a creature with a boast ability after that creature attacked, you can activate that creature's boast ability if it hasn't been activated yet that turn.")
        ruling("2021-02-05", "If a creature with a boast ability is put onto the battlefield attacking, it was never declared as an attacker. Its boast ability can't be activated that turn.")
        ruling("2021-02-05", "If an effect adds additional combat phases to a turn and a creature with a boast ability attacks more than once during that turn, its boast ability can still be activated only once.")
    }
}
