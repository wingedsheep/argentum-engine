package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DealDamageEffect
import com.wingedsheep.sdk.scripting.DynamicAmount
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.EntersWithCreatureTypeChoice
import com.wingedsheep.sdk.targeting.AnyTarget

/**
 * Doom Cannon
 * {6}
 * Artifact
 * As Doom Cannon enters the battlefield, choose a creature type.
 * {3}, {T}, Sacrifice a creature of the chosen type: Doom Cannon deals 3 damage to any target.
 */
val DoomCannon = card("Doom Cannon") {
    manaCost = "{6}"
    typeLine = "Artifact"
    oracleText = "As Doom Cannon enters the battlefield, choose a creature type.\n{3}, {T}, Sacrifice a creature of the chosen type: Doom Cannon deals 3 damage to any target."

    replacementEffect(EntersWithCreatureTypeChoice())

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{3}"),
            Costs.Tap,
            Costs.SacrificeChosenCreatureType
        )
        target = AnyTarget()
        effect = DealDamageEffect(
            amount = DynamicAmount.Fixed(3),
            target = EffectTarget.ContextTarget(0)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "307"
        artist = "Matthew Mitchell"
        flavorText = "\"It was built to end things, and end things it does.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4abde0d7-266b-41bd-ade1-c4d93507eb16.jpg?1562910764"
    }
}
