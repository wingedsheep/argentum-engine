package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Curse of Thirst
 * {4}{B}
 * Enchantment — Aura Curse
 * Enchant player
 * At the beginning of enchanted player's upkeep, this Aura deals damage to that player equal to the
 * number of Curses attached to them.
 *
 * The count is [DynamicAmounts.cursesAttachedToEnchantedPlayer] (Curse of Surveillance's), read on
 * resolution and including this Curse itself.
 */
val CurseOfThirst = card("Curse of Thirst") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura Curse"
    oracleText = "Enchant player\nAt the beginning of enchanted player's upkeep, this Aura deals damage to " +
        "that player equal to the number of Curses attached to them."

    auraTarget = Targets.Player

    triggeredAbility {
        trigger = Triggers.player(Player.EnchantedPlayer).beginningOf(Step.UPKEEP)
        effect = Effects.DealDamage(
            DynamicAmounts.cursesAttachedToEnchantedPlayer(),
            EffectTarget.PlayerRef(Player.EnchantedPlayer)
        )
        description = "At the beginning of enchanted player's upkeep, this Aura deals damage to that player " +
            "equal to the number of Curses attached to them."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "57"
        artist = "Christopher Moeller"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a23ed5d1-44dc-4733-9e01-65fbc5dc02f2.jpg?1783940833"
    }
}
