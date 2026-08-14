package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect

/**
 * Loxodon Mender — Mirrodin #12
 * {5}{W} · Creature — Elephant Cleric · 3/3
 *
 * {W}, {T}: Regenerate target artifact.
 *
 * Modelling notes:
 * - Targets *any* artifact, not just artifact creatures — a regeneration shield on a
 *   noncreature artifact does nothing until something would destroy it, which is exactly
 *   the printed behaviour (CR 701.15). [Targets.Artifact] is the permanent-scoped filter.
 * - [RegenerateEffect] drops the shield until end of turn; it is not a regeneration
 *   *ability* on the artifact, so "can't be regenerated" markers still win.
 */
val LoxodonMender = card("Loxodon Mender") {
    manaCost = "{5}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Elephant Cleric"
    power = 3
    toughness = 3
    oracleText = "{W}, {T}: Regenerate target artifact."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}"), Costs.Tap)
        val artifact = target("target artifact", Targets.Artifact)
        effect = RegenerateEffect(artifact)
        description = "{W}, {T}: Regenerate target artifact."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "12"
        artist = "Heather Hudson"
        flavorText = "The Auriok believe that in the hands of a loxodon, no weapon can be broken."
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c17ac7c1-6d53-40b4-921f-4e23e4026041.jpg?1783944561"
    }
}
