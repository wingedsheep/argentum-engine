package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Blow Your House Down
 * {2}{R}
 * Sorcery
 *
 * Up to three target creatures can't block this turn. Destroy any of them that are Walls.
 *
 * "Up to three target creatures" is one requirement with `count = 3, optional = true`; the body runs
 * once per still-legal target through `ForEachTarget`, so inside it `ContextTarget(0)` is the creature
 * being visited. "Any of them that are Walls" tests each *target* (not every Wall on the battlefield)
 * at resolution, against projected subtypes.
 */
val BlowYourHouseDown = card("Blow Your House Down") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Up to three target creatures can't block this turn. Destroy any of them that are Walls."

    spell {
        target = TargetObject(filter = TargetFilter.Creature, count = 3, optional = true)
        effect = Effects.ForEachTarget(
            Effects.CantBlock(target = EffectTarget.ContextTarget(0)),
            Effects.If(
                Conditions.EntityMatches(
                    EffectTarget.ContextTarget(0),
                    GameObjectFilter.Creature.withSubtype(Subtype.WALL)
                ),
                Effects.Destroy(EffectTarget.ContextTarget(0))
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "114"
        artist = "Ravenna Tran"
        flavorText = "\"You think these walls will protect you? My, you have a dim view of the power of " +
            "the wilds.\"\n—Marawen, barrow witch"
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c04b85e7-a401-42d5-9629-8d4b8c8a46b0.jpg?1783932628"
    }
}
