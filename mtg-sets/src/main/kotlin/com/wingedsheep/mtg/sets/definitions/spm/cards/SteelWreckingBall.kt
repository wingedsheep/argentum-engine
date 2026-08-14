package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Steel Wrecking Ball
 * {5}
 * Artifact
 *
 * When this artifact enters, it deals 5 damage to target creature.
 * {1}{R}, Discard this card: Destroy target artifact.
 *
 * The second ability functions from the hand: its cost discards this card
 * ([Costs.DiscardSelf]) and it's activated from the hand zone
 * ([activateFromZone] = [Zone.HAND]) — a from-hand artifact-removal you can use while
 * this card is stranded in hand (cf. Spinewoods Armadillo, Harvester of Misery).
 */
val SteelWreckingBall = card("Steel Wrecking Ball") {
    manaCost = "{5}"
    colorIdentity = "R"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, it deals 5 damage to target creature.\n" +
        "{1}{R}, Discard this card: Destroy target artifact."

    // When this artifact enters, it deals 5 damage to target creature.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target("target creature", Targets.Creature)
        effect = Effects.DealDamage(5, creature)
    }

    // {1}{R}, Discard this card (from hand): Destroy target artifact.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{R}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        val artifact = target("target artifact", Targets.Artifact)
        effect = Effects.Destroy(artifact)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "177"
        artist = "Michele Giorgi"
        flavorText = "\"Heads up! It's time for a fast game of catch!\""
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f2c74f5-1cfe-4918-a86b-0d58ac8b7469.jpg?1783905300"
    }
}
