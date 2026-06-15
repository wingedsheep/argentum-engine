package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Grapeshot Catapult
 * {4}
 * Artifact Creature — Construct
 * 2/3
 * {T}: This creature deals 1 damage to target creature with flying.
 */
val GrapeshotCatapult = card("Grapeshot Catapult") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    power = 2
    toughness = 3
    oracleText = "{T}: This creature deals 1 damage to target creature with flying."

    activatedAbility {
        cost = Costs.Tap
        val t = target("target creature with flying", Targets.CreatureWithKeyword(Keyword.FLYING))
        effect = Effects.DealDamage(1, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "52"
        artist = "Dan Frazier"
        flavorText = "For years scholars debated whether these were Urza's or Mishra's creations. Recent research suggests they were invented by the brothers' original master, Tocasia, and that both used these devices."
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4c7a7348-c82e-453c-975c-e5365e152a3a.jpg?1562911063"
    }
}
