package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fathom Fleet Boarder
 * {2}{B}
 * Creature — Orc Pirate
 * 3/3
 * When this creature enters, you lose 2 life unless you control another Pirate.
 *
 * "Unless" is checked only as the ability resolves (the ability always triggers), so it is an
 * [Effects.If] gate on the effect rather than an intervening-if.
 */
val FathomFleetBoarder = card("Fathom Fleet Boarder") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Orc Pirate"
    oracleText = "When this creature enters, you lose 2 life unless you control another Pirate."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.If(
            condition = Conditions.YouControl(
                GameObjectFilter.Permanent.withSubtype(Subtype.PIRATE),
                negate = true,
                excludeSelf = true
            ),
            then = Effects.LoseLife(2, EffectTarget.Controller)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "71"
        artist = "Sidharth Chaturvedi"
        flavorText = "Without frequent raids, orcs sometimes get bored and plunder their own ships."
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f447eb83-c805-4ba8-b9f2-0add092f6795.jpg?1783935311"
        ruling(
            "2018-01-19",
            "Fathom Fleet Boarder's ability triggers regardless of whether you control another " +
                "Pirate. Whether you control another Pirate is checked only as the ability resolves."
        )
    }
}
