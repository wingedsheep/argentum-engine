package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Collapsing Borders
 * {3}{R}
 * Enchantment
 * Domain — At the beginning of each player's upkeep, that player gains 1 life for each
 * basic land type among lands they control. Then this enchantment deals 3 damage to that player.
 */
val CollapsingBorders = card("Collapsing Borders") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Domain — At the beginning of each player's upkeep, that player gains 1 life for each " +
        "basic land type among lands they control. Then this enchantment deals 3 damage to that player."

    triggeredAbility {
        trigger = Triggers.EachUpkeep
        // Domain is computed for the upkeep player (the lands *they* control), and both the
        // life gain and the 3 damage are applied to that same player.
        effect = Effects.GainLife(
            DynamicAmounts.domain(Player.TriggeringPlayer),
            EffectTarget.PlayerRef(Player.TriggeringPlayer)
        ) then Effects.DealDamage(3, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "141"
        artist = "Glen Angus"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc019633-788e-4095-9610-6c0a432f7656.jpg?1562936001"
    }
}
