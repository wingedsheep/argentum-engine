package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Usher of the Fallen — Kaldheim #35 (canonical printing)
 * {W} · Creature — Spirit Warrior · 2/1
 *
 * Boast — {1}{W}: Create a 1/1 white Human Warrior creature token.
 */
val UsherOfTheFallen = card("Usher of the Fallen") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Spirit Warrior"
    power = 2
    toughness = 1
    oracleText = "Boast — {1}{W}: Create a 1/1 white Human Warrior creature token. (Activate only if " +
        "this creature attacked this turn and only once each turn.)"

    activatedAbility {
        isBoast = true
        cost = Costs.Mana("{1}{W}")
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Human", "Warrior")
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "35"
        artist = "Anastasia Ovchinnikova"
        flavorText = "\"I have led ten thousand through these mists and never lost the way.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/4/24e58478-c6a8-4f86-854a-a489c99bd777.jpg?1783928276"
        ruling("2021-02-05", "A boast ability can be activated at any point after the creature with that ability has been declared as an attacker. This can be before blockers are declared, after blockers are declared but before combat damage is dealt, during combat after combat damage is dealt, during the postcombat main phase, during the end step, or, in some unusual cases, during the cleanup step.")
        ruling("2021-02-05", "If it's not your turn and you gain control of a creature with a boast ability after that creature attacked, you can activate that creature's boast ability if it hasn't been activated yet that turn.")
        ruling("2021-02-05", "If a creature with a boast ability is put onto the battlefield attacking, it was never declared as an attacker. Its boast ability can't be activated that turn.")
        ruling("2021-02-05", "If an effect adds additional combat phases to a turn and a creature with a boast ability attacks more than once during that turn, its boast ability can still be activated only once.")
    }
}
