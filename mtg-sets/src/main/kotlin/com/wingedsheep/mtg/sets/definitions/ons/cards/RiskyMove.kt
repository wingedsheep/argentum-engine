package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.FlipCoinEffect
import com.wingedsheep.sdk.scripting.effects.GainControlByActivePlayerEffect
import com.wingedsheep.sdk.scripting.effects.GiveControlToTargetPlayerEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Risky Move
 * {3}{R}{R}{R}
 * Enchantment
 *
 * At the beginning of each player's upkeep, that player gains control of Risky Move.
 * When you gain control of Risky Move from another player, choose a creature you control
 * and an opponent. Flip a coin. If you lose the flip, that opponent gains control of
 * that creature.
 */
val RiskyMove = card("Risky Move") {
    manaCost = "{3}{R}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "At the beginning of each player's upkeep, that player gains control of Risky Move.\n" +
        "When you gain control of Risky Move from another player, choose a creature you control and an opponent. " +
        "Flip a coin. If you lose the flip, that opponent gains control of that creature."

    // Ability 1: At the beginning of each player's upkeep, that player gains control of this.
    triggeredAbility {
        trigger = Triggers.EachUpkeep
        effect = GainControlByActivePlayerEffect(EffectTarget.Self)
    }

    // Ability 2: When you gain control of this from another player, choose a creature
    // you control and an opponent. Flip a coin. If you lose, opponent gets the creature.
    triggeredAbility {
        trigger = Triggers.GainControlOfSelf
        val t = target("target", Targets.CreatureYouControl)
        // "Choose ... an opponent" is a non-targeted choice; until the multiplayer
        // choose-an-opponent flow exists (backlog/multiplayer.md), AnOpponent resolves
        // to the first opponent in turn order — exact in two-player games.
        effect = FlipCoinEffect(
            lostEffect = GiveControlToTargetPlayerEffect(
                permanent = t,
                newController = EffectTarget.PlayerRef(Player.AnOpponent)
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "223"
        artist = "Pete Venters"
        flavorText = "\"Let's raise the stakes.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b09315c-d6ff-4fdb-8774-c6402b45e959.jpg?1562897507"
    }
}
