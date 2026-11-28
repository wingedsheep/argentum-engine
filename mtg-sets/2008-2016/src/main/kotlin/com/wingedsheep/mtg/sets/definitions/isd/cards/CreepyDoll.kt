package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Creepy Doll
 * {5}
 * Artifact Creature — Construct
 * 1/1
 * Indestructible
 * Whenever this creature deals combat damage to a creature, flip a coin. If you win the flip,
 * destroy that creature.
 */
val CreepyDoll = card("Creepy Doll") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    power = 1
    toughness = 1
    oracleText = "Indestructible\nWhenever this creature deals combat damage to a creature, flip a coin. " +
        "If you win the flip, destroy that creature."

    keywords(Keyword.INDESTRUCTIBLE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyCreature)
        effect = Effects.FlipCoin(
            wonEffect = Effects.Destroy(EffectTarget.TriggeringEntity),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "220"
        artist = "Matt Stewart"
        flavorText = "A child's porcelain doll went missing one night, as did a pair of kitchen shears " +
            "and the town magistrate."
        imageUri =
            "https://cards.scryfall.io/normal/front/7/6/762a598b-8753-47ec-9dd6-2c3d8882fda6.jpg?1783940901"
        ruling(
            "2011-09-22",
            "You don't flip the coin until the ability resolves. If you want to respond to the ability, " +
                "perhaps by regenerating the damaged creature, you'll have to do so before you know the " +
                "outcome of the flip.",
        )
        ruling(
            "2011-09-22",
            "If the combat damage Creepy Doll deals to a creature is lethal, you'll still flip a coin. " +
                "If the creature is still on the battlefield (perhaps because it regenerated), it could be " +
                "destroyed a second time, depending on the coin flip.",
        )
    }
}
