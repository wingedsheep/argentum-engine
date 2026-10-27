package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Hagi Mob — Kaldheim #140 (canonical printing)
 * {4}{R} · Creature — Troll Berserker · 5/4
 *
 * Boast — {1}{R}: This creature deals 1 damage to any target. (Activate only if this creature
 * attacked this turn and only once each turn.)
 */
val HagiMob = card("Hagi Mob") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Troll Berserker"
    power = 5
    toughness = 4
    oracleText = "Boast — {1}{R}: This creature deals 1 damage to any target. (Activate only if this " +
        "creature attacked this turn and only once each turn.)"

    activatedAbility {
        isBoast = true
        cost = Costs.Mana("{1}{R}")
        val t = target(Targets.Any)
        effect = Effects.DealDamage(1, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "140"
        artist = "Steve Prescott"
        flavorText = "\"Graarg!\""
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d3b18cdb-bc17-42ae-bfb7-4eafce01cd39.jpg?1783928228"
        ruling("2021-02-05", "A boast ability can be activated at any point after the creature with that ability has been declared as an attacker. This can be before blockers are declared, after blockers are declared but before combat damage is dealt, during combat after combat damage is dealt, during the postcombat main phase, during the end step, or, in some unusual cases, during the cleanup step.")
        ruling("2021-02-05", "If it's not your turn and you gain control of a creature with a boast ability after that creature attacked, you can activate that creature's boast ability if it hasn't been activated yet that turn.")
        ruling("2021-02-05", "If a creature with a boast ability is put onto the battlefield attacking, it was never declared as an attacker. Its boast ability can't be activated that turn.")
        ruling("2021-02-05", "If an effect adds additional combat phases to a turn and a creature with a boast ability attacks more than once during that turn, its boast ability can still be activated only once.")
    }
}
