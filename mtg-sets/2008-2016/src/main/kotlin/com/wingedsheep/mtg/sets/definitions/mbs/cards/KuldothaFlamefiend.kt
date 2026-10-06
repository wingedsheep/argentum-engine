package com.wingedsheep.mtg.sets.definitions.mbs.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Kuldotha Flamefiend
 * {4}{R}{R}
 * Creature — Elemental
 * 4/4
 * When this creature enters, you may sacrifice an artifact. If you do, this creature deals 4 damage
 * divided as you choose among any number of targets.
 *
 * The targets and the division are announced as the trigger goes on the stack (CR 603.3d); the
 * artifact is sacrificed as it resolves. At least 1 damage per target caps "any number" at four.
 */
val KuldothaFlamefiend = card("Kuldotha Flamefiend") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental"
    oracleText = "When this creature enters, you may sacrifice an artifact. If you do, this creature " +
        "deals 4 damage divided as you choose among any number of targets."
    power = 4
    toughness = 4

    triggeredAbility {
        trigger = Triggers.self.enters()
        target = AnyTarget(count = 4, minCount = 0, optional = true)
        effect = Effects.MayPay(
            cost = Effects.SacrificeOwn(filter = GameObjectFilter.Artifact),
            then = Effects.DividedDamage(total = 4, minTargets = 0, maxTargets = 4),
            descriptionOverride = "You may sacrifice an artifact. If you do, this creature deals 4 damage " +
                "divided as you chose among the targets."
        )
        description = "You may sacrifice an artifact. If you do, this creature deals 4 damage divided " +
            "as you choose among any number of targets."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "69"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/189fea03-24db-4574-bbc2-4d3bc9e629a5.jpg?1783941378"
    }
}
