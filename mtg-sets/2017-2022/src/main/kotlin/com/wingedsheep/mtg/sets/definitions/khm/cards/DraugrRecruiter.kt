package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Draugr Recruiter — Kaldheim #87 (canonical printing)
 * {3}{B} · Creature — Zombie Cleric · 3/3
 *
 * Boast — {3}{B}: Return target creature card from your graveyard to your hand.
 */
val DraugrRecruiter = card("Draugr Recruiter") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Cleric"
    power = 3
    toughness = 3
    oracleText = "Boast — {3}{B}: Return target creature card from your graveyard to your hand. " +
        "(Activate only if this creature attacked this turn and only once each turn.)"

    activatedAbility {
        isBoast = true
        cost = Costs.Mana("{3}{B}")
        val t = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.ReturnToHand(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "87"
        artist = "Colin Boyer"
        flavorText = "\"You cannot stand against us, but when you fall, you will stand with us!\""
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b0fe66b-0067-4e5c-9b3a-b014b2f0daf2.jpg?1783928250"
        ruling("2021-02-05", "A boast ability can be activated at any point after the creature with that ability has been declared as an attacker. This can be before blockers are declared, after blockers are declared but before combat damage is dealt, during combat after combat damage is dealt, during the postcombat main phase, during the end step, or, in some unusual cases, during the cleanup step.")
        ruling("2021-02-05", "If it's not your turn and you gain control of a creature with a boast ability after that creature attacked, you can activate that creature's boast ability if it hasn't been activated yet that turn.")
        ruling("2021-02-05", "If a creature with a boast ability is put onto the battlefield attacking, it was never declared as an attacker. Its boast ability can't be activated that turn.")
        ruling("2021-02-05", "If an effect adds additional combat phases to a turn and a creature with a boast ability attacks more than once during that turn, its boast ability can still be activated only once.")
    }
}
