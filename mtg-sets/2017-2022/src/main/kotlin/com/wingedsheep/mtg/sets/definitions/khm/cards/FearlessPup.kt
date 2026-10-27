package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fearless Pup — Kaldheim #136 (canonical printing)
 * {R} · Creature — Wolf · 1/1
 *
 * First strike
 * Boast — {2}{R}: This creature gets +2/+0 until end of turn.
 */
val FearlessPup = card("Fearless Pup") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Wolf"
    power = 1
    toughness = 1
    oracleText = "First strike\n" +
        "Boast — {2}{R}: This creature gets +2/+0 until end of turn. " +
        "(Activate only if this creature attacked this turn and only once each turn.)"

    keywords(Keyword.FIRST_STRIKE)

    activatedAbility {
        isBoast = true
        cost = Costs.Mana("{2}{R}")
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "136"
        artist = "Jehan Choo"
        flavorText = "\"Awooo!\""
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0eba2250-0e9b-46fd-a4b5-23f30329e063.jpg?1783928230"
        ruling("2021-02-05", "A boast ability can be activated at any point after the creature with that ability has been declared as an attacker. This can be before blockers are declared, after blockers are declared but before combat damage is dealt, during combat after combat damage is dealt, during the postcombat main phase, during the end step, or, in some unusual cases, during the cleanup step.")
        ruling("2021-02-05", "If it's not your turn and you gain control of a creature with a boast ability after that creature attacked, you can activate that creature's boast ability if it hasn't been activated yet that turn.")
        ruling("2021-02-05", "If a creature with a boast ability is put onto the battlefield attacking, it was never declared as an attacker. Its boast ability can't be activated that turn.")
        ruling("2021-02-05", "If an effect adds additional combat phases to a turn and a creature with a boast ability attacks more than once during that turn, its boast ability can still be activated only once.")
    }
}
