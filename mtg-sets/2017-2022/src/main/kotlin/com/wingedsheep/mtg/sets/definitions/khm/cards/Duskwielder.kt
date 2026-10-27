package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Duskwielder — Kaldheim #91 (canonical printing)
 * {B} · Creature — Elf Berserker · 1/2
 *
 * Boast — {1}: Target opponent loses 1 life and you gain 1 life.
 */
val Duskwielder = card("Duskwielder") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Elf Berserker"
    power = 1
    toughness = 2
    oracleText = "Boast — {1}: Target opponent loses 1 life and you gain 1 life. " +
        "(Activate only if this creature attacked this turn and only once each turn.)"

    activatedAbility {
        isBoast = true
        cost = Costs.Mana("{1}")
        val opponent = target(Targets.Opponent)
        effect = Effects.LoseLife(1, opponent) then Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "91"
        artist = "Daarken"
        flavorText = "\"My shadow alone will slay you.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b5490b8-5652-40f5-8678-e1263ef69b5a.jpg?1783928250"
        ruling("2021-02-05", "A boast ability can be activated at any point after the creature with that ability has been declared as an attacker. This can be before blockers are declared, after blockers are declared but before combat damage is dealt, during combat after combat damage is dealt, during the postcombat main phase, during the end step, or, in some unusual cases, during the cleanup step.")
        ruling("2021-02-05", "If it's not your turn and you gain control of a creature with a boast ability after that creature attacked, you can activate that creature's boast ability if it hasn't been activated yet that turn.")
        ruling("2021-02-05", "If a creature with a boast ability is put onto the battlefield attacking, it was never declared as an attacker. Its boast ability can't be activated that turn.")
        ruling("2021-02-05", "If an effect adds additional combat phases to a turn and a creature with a boast ability attacks more than once during that turn, its boast ability can still be activated only once.")
    }
}
