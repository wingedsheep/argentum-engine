package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Gang of Devils
 * {5}{R}
 * Creature — Devil
 * 3/3
 *
 * When this creature dies, it deals 3 damage divided as you choose among one, two, or three targets.
 *
 * Targets and the division are chosen as the trigger goes on the stack, each target getting at
 * least 1 damage (the Inferno Titan shape on a dies trigger). The departed creature is the
 * damage source via its last-known information.
 */
val GangOfDevils = card("Gang of Devils") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Devil"
    power = 3
    toughness = 3
    oracleText = "When this creature dies, it deals 3 damage divided as you choose among one, two, or three targets."

    triggeredAbility {
        trigger = Triggers.self.dies()
        target = AnyTarget(count = 3, minCount = 1)
        effect = Effects.DividedDamage(total = 3, minTargets = 1, maxTargets = 3)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "136"
        artist = "Erica Yang"
        flavorText = "\"Well, how could I know they'd explode?\"\n—Gregel, township militia"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/0430b9fa-3bc6-4183-ad5b-d70ad401fa97.jpg?1783940684"
        ruling(
            "2012-05-01",
            "You divide the damage as you put Gang of Devils's triggered ability on the stack, not as it " +
                "resolves. Each target must be assigned at least 1 damage.",
        )
    }
}
