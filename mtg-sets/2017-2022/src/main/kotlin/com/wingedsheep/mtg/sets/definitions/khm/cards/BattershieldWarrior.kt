package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Battershield Warrior — Kaldheim #2 (canonical printing)
 * {2}{W} · Creature — Human Warrior · 2/2
 *
 * Boast — {1}{W}: Creatures you control get +1/+1 until end of turn. (Activate only if this
 * creature attacked this turn and only once each turn.)
 */
val BattershieldWarrior = card("Battershield Warrior") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 2
    oracleText = "Boast — {1}{W}: Creatures you control get +1/+1 until end of turn. (Activate only if " +
        "this creature attacked this turn and only once each turn.)"

    activatedAbility {
        isBoast = true
        cost = Costs.Mana("{1}{W}")
        effect = Patterns.Group.modifyStatsForAll(1, 1, GroupFilter.AllCreaturesYouControl)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "2"
        artist = "Colin Boyer"
        flavorText = "\"I need no axe to best these weaklings.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb79dc85-f85a-4846-9ac3-c75c64ea9c0a.jpg?1783928292"
        ruling("2021-02-05", "A boast ability can be activated at any point after the creature with that ability has been declared as an attacker. This can be before blockers are declared, after blockers are declared but before combat damage is dealt, during combat after combat damage is dealt, during the postcombat main phase, during the end step, or, in some unusual cases, during the cleanup step.")
        ruling("2021-02-05", "If a creature with a boast ability is put onto the battlefield attacking, it was never declared as an attacker. Its boast ability can't be activated that turn.")
        ruling("2021-02-05", "If an effect adds additional combat phases to a turn and a creature with a boast ability attacks more than once during that turn, its boast ability can still be activated only once.")
    }
}
