package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Boulder Salvo
 * {4}{R}
 * Sorcery
 * Surge {1}{R} (You may cast this spell for its surge cost if you or a teammate has cast another spell this turn.)
 * Boulder Salvo deals 4 damage to target creature.
 *
 * Surge here is only an alternative cost; the spell does the same thing either way.
 */
val BoulderSalvo = card("Boulder Salvo") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Surge {1}{R} (You may cast this spell for its surge cost if you or a teammate has cast another spell this turn.)\n" +
        "Boulder Salvo deals 4 damage to target creature."

    keywordAbility(KeywordAbility.surge("{1}{R}"))

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(4, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "102"
        artist = "Vincent Proce"
        flavorText = "\"If someone claims the sky is falling, I listen.\"\n—Yon Basrel, Oran-Rief survivalist"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4e269989-bb22-4da4-a374-434a572e8e8f.jpg?1783937908"
    }
}
