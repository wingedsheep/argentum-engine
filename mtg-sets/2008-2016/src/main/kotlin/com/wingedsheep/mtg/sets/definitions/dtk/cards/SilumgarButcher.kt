package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.exploit
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Silumgar Butcher
 * {4}{B}
 * Creature — Zombie Djinn
 * 3/3
 * Exploit (When this creature enters, you may sacrifice a creature.)
 * When this creature exploits a creature, target creature gets -3/-3 until end of turn.
 *
 * The targeted shrink rides the exploit reflexive, so the creature is chosen after the sacrifice
 * resolves.
 */
val SilumgarButcher = card("Silumgar Butcher") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Djinn"
    power = 3
    toughness = 3
    oracleText = "Exploit (When this creature enters, you may sacrifice a creature.)\n" +
        "When this creature exploits a creature, target creature gets -3/-3 until end of turn."

    exploit {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(-3, -3, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "122"
        artist = "Dave Kendall"
        flavorText = "Silumgar takes pride in the diversity of his sibsig."
        imageUri = "https://cards.scryfall.io/normal/front/4/0/40cb67f7-b4e1-423b-8f55-d44ed383e778.jpg"
    }
}
