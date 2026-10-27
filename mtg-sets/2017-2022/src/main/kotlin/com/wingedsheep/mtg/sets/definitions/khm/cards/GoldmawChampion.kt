package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Goldmaw Champion — Kaldheim #14 (canonical printing)
 * {2}{W} · Creature — Dwarf Warrior · 2/3
 *
 * Boast — {1}{W}: Tap target creature.
 */
val GoldmawChampion = card("Goldmaw Champion") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dwarf Warrior"
    power = 2
    toughness = 3
    oracleText = "Boast — {1}{W}: Tap target creature. (Activate only if this creature attacked this " +
        "turn and only once each turn.)"

    activatedAbility {
        isBoast = true
        cost = Costs.Mana("{1}{W}")
        val creature = target(TargetFilter.Creature)
        effect = Effects.Tap(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "14"
        artist = "Slawomir Maniak"
        flavorText = "\"That was nothing. I once took down a frost giant with a spoon!\""
        imageUri = "https://cards.scryfall.io/normal/front/4/4/44d4cd64-8560-48f5-b0bc-f75286c1c91c.jpg?1783928283"
        ruling("2021-02-05", "A boast ability can be activated at any point after the creature with that ability has been declared as an attacker. This can be before blockers are declared, after blockers are declared but before combat damage is dealt, during combat after combat damage is dealt, during the postcombat main phase, during the end step, or, in some unusual cases, during the cleanup step.")
        ruling("2021-02-05", "If it's not your turn and you gain control of a creature with a boast ability after that creature attacked, you can activate that creature's boast ability if it hasn't been activated yet that turn.")
        ruling("2021-02-05", "If a creature with a boast ability is put onto the battlefield attacking, it was never declared as an attacker. Its boast ability can't be activated that turn.")
        ruling("2021-02-05", "If an effect adds additional combat phases to a turn and a creature with a boast ability attacks more than once during that turn, its boast ability can still be activated only once.")
    }
}
