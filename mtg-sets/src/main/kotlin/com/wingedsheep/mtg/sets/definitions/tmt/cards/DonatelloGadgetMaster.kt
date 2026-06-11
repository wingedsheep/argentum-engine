package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Donatello, Gadget Master
 * {2}{U}
 * Legendary Creature — Mutant Ninja Turtle
 * 3/2
 *
 * Sneak {1}{U} (You may cast this spell for {1}{U} if you also return an
 * unblocked attacker you control to hand during the declare blockers step. He
 * enters tapped and attacking.)
 * Whenever Donatello deals combat damage to a player, create a token that's a
 * copy of target artifact you control.
 */
val DonatelloGadgetMaster = card("Donatello, Gadget Master") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Mutant Ninja Turtle"
    oracleText = "Sneak {1}{U} (You may cast this spell for {1}{U} if you also return an unblocked attacker you control to hand during the declare blockers step. He enters tapped and attacking.)\nWhenever Donatello deals combat damage to a player, create a token that's a copy of target artifact you control."
    power = 3
    toughness = 2

    sneak("{1}{U}")

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        val artifact = target(
            "target artifact you control",
            TargetPermanent(filter = TargetFilter(GameObjectFilter.Artifact.youControl()))
        )
        effect = Effects.CreateTokenCopyOfTarget(artifact)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "35"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b5dd830-dab8-4628-845f-3d17973f9ffa.jpg?1769005622"
    }
}
