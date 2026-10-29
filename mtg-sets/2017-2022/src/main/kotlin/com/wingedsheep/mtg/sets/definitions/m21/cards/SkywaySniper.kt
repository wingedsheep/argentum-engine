package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Skyway Sniper — Core Set 2021 #206 (canonical printing)
 * {G} · Creature — Elf Archer · 1/2
 *
 * Reach
 * {2}{G}: This creature deals 1 damage to target creature with flying.
 */
val SkywaySniper = card("Skyway Sniper") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Archer"
    power = 1
    toughness = 2
    oracleText = "Reach (This creature can block creatures with flying.)\n" +
        "{2}{G}: This creature deals 1 damage to target creature with flying."

    keywords(Keyword.REACH)

    // Trophy Hunter's shape: "with flying" is a targeting predicate; the ability's source deals the damage.
    activatedAbility {
        cost = Costs.Mana("{2}{G}")
        val creature = target(TargetFilter.Creature.withKeyword(Keyword.FLYING))
        effect = Effects.DealDamage(1, creature)
        description = "{2}{G}: This creature deals 1 damage to target creature with flying."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "206"
        artist = "Even Amundsen"
        flavorText = "\"No sight is more beautiful than a clear sky.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/7/1776f5b4-1292-460f-9719-e1b603cee46c.jpg?1783930666"
    }
}
