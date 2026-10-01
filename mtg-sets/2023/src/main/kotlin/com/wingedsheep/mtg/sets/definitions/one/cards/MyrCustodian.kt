package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Myr Custodian
 * {3}
 * Artifact Creature — Myr
 * 2/3
 *
 * When this creature enters, scry 2. Then each opponent may scry 1.
 *
 * The opponents' half is `ForEachPlayer(EachOpponent)`, which rebinds the resolving controller to
 * the opponent being asked — so the yes/no and the scry both belong to (and read the library of)
 * that opponent.
 */
val MyrCustodian = card("Myr Custodian") {
    manaCost = "{3}"
    typeLine = "Artifact Creature — Myr"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, scry 2. Then each opponent may scry 1. " +
        "(To scry X, a player looks at the top X cards of their library, then puts any number of " +
        "them on the bottom and the rest on top in any order.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Scry(2) then
            Effects.ForEachPlayer(
                Player.EachOpponent,
                listOf(
                    Effects.May(
                        effect = Effects.Scry(1),
                        decisionMaker = EffectTarget.PlayerRef(Player.You),
                        descriptionOverride = "Scry 1?",
                    ),
                ),
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "235"
        artist = "Mark Behm"
        flavorText = "\"Myr will go where the work is. We always have. We always will.\"\n—Urtet, Remnant of Memnarch"
        imageUri = "https://cards.scryfall.io/normal/front/5/6/56bfdd21-4d46-4ec2-ba25-f16b6993f1a8.jpg?1783917988"
    }
}
