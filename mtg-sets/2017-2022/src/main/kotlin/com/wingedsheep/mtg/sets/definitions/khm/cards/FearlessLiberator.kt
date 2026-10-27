package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Fearless Liberator — Kaldheim #135 (canonical printing)
 * {1}{R} · Creature — Dwarf Berserker · 2/1
 *
 * Boast — {2}{R}: Create a 2/1 red Dwarf Berserker creature token.
 * (Activate only if this creature attacked this turn and only once each turn.)
 */
val FearlessLiberator = card("Fearless Liberator") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dwarf Berserker"
    power = 2
    toughness = 1
    oracleText = "Boast — {2}{R}: Create a 2/1 red Dwarf Berserker creature token. (Activate only if " +
        "this creature attacked this turn and only once each turn.)"

    activatedAbility {
        isBoast = true
        cost = Costs.Mana("{2}{R}")
        effect = Effects.CreateToken(
            power = 2,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Dwarf", "Berserker")
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "135"
        artist = "Zezhou Chen"
        flavorText = "\"No one else had the spine for it, so I ran right between that giant's legs and hauled poor Valdir back to Axgard!\""
        imageUri = "https://cards.scryfall.io/normal/front/2/1/212462b7-e23e-4c87-93aa-9cadefbf1ac8.jpg?1783928231"
        ruling("2021-02-05", "A boast ability can be activated at any point after the creature with that ability has been declared as an attacker. This can be before blockers are declared, after blockers are declared but before combat damage is dealt, during combat after combat damage is dealt, during the postcombat main phase, during the end step, or, in some unusual cases, during the cleanup step.")
        ruling("2021-02-05", "If it's not your turn and you gain control of a creature with a boast ability after that creature attacked, you can activate that creature's boast ability if it hasn't been activated yet that turn.")
        ruling("2021-02-05", "If a creature with a boast ability is put onto the battlefield attacking, it was never declared as an attacker. Its boast ability can't be activated that turn.")
        ruling("2021-02-05", "If an effect adds additional combat phases to a turn and a creature with a boast ability attacks more than once during that turn, its boast ability can still be activated only once.")
    }
}
