package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sewer Crocodile
 * {5}{U}
 * Creature — Crocodile
 * 4/6
 *
 * {3}{U}: This creature can't be blocked this turn. This ability costs {3} less to activate if
 * there are five or more mana values among cards in your graveyard.
 *
 * The discount is `costsLessIf` (Razorlash Transmogrant) gated on
 * [Conditions.DistinctManaValuesInGraveyard] (Syndicate Infiltrator).
 */
val SewerCrocodile = card("Sewer Crocodile") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Crocodile"
    power = 4
    toughness = 6
    oracleText = "{3}{U}: This creature can't be blocked this turn. This ability costs {3} less to " +
        "activate if there are five or more mana values among cards in your graveyard."

    activatedAbility {
        cost = Costs.Mana("{3}{U}")
        costsLessIf("{3}", Conditions.DistinctManaValuesInGraveyard(5))
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self)
        description = "{3}{U}: This creature can't be blocked this turn. This ability costs {3} " +
            "less to activate if there are five or more mana values among cards in your graveyard."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "60"
        artist = "Milivoj Ćeran"
        flavorText = "\"Quick, let's hide in the sewer!\"\n—Ginsi the Thief, now deceased"
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a06ac58b-6d72-4eb4-a1c9-61b5314c7769.jpg?1783923137"
    }
}
