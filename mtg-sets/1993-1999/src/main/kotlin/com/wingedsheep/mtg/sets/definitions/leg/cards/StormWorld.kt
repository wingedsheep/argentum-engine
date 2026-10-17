package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Storm World
 * {R}
 * World Enchantment
 *
 * At the beginning of each player's upkeep, this enchantment deals X damage to that player,
 * where X is 4 minus the number of cards in their hand.
 *
 * A negative X deals no damage. The world supertype puts it under the world rule (CR 704.5k).
 */
val StormWorld = card("Storm World") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "World Enchantment"
    oracleText = "At the beginning of each player's upkeep, this enchantment deals X damage to that " +
        "player, where X is 4 minus the number of cards in their hand."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        effect = Effects.DealDamage(
            amount = 4 - DynamicAmounts.zone(Player.TriggeringPlayer, Zone.HAND).count(),
            target = EffectTarget.PlayerRef(Player.TriggeringPlayer)
        )
        description = "At the beginning of each player's upkeep, Storm World deals damage to that " +
            "player equal to 4 minus the number of cards in their hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "165"
        artist = "Christopher Rush"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cdf0afa6-7003-454d-9b8a-e3328aaf29ed.jpg?1783948052"
    }
}
