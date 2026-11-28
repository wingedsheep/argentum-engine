package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Curse of the Pierced Heart
 * {1}{R}
 * Enchantment — Aura Curse
 * Enchant player
 * At the beginning of enchanted player's upkeep, this Aura deals 1 damage to that player or a
 * planeswalker that player controls.
 *
 * The Curse's controller chooses the recipient as the trigger resolves (nothing is targeted): one of
 * the enchanted player's planeswalkers, or none to deal the damage to the player. With no
 * planeswalker to choose there is no prompt.
 */
val CurseOfThePiercedHeart = card("Curse of the Pierced Heart") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura Curse"
    oracleText = "Enchant player\nAt the beginning of enchanted player's upkeep, this Aura deals 1 damage " +
        "to that player or a planeswalker that player controls."

    auraTarget = Targets.Player

    triggeredAbility {
        trigger = Triggers.player(Player.EnchantedPlayer).beginningOf(Step.UPKEEP)
        effect = Effects.Pipeline {
            val planeswalker = selectTarget(
                TargetObject(
                    optional = true,
                    filter = TargetFilter(GameObjectFilter.Planeswalker.controlledByEnchantedPlayer())
                ),
                nonTargeting = true,
                prompt = "Choose a planeswalker enchanted player controls to deal the damage to, " +
                    "or none to deal it to the player"
            )
            ifNotEmpty(planeswalker) {
                run(Effects.DealDamage(1, planeswalker.asTarget))
            } orElse {
                run(Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EnchantedPlayer)))
            }
        }
        description = "At the beginning of enchanted player's upkeep, this Aura deals 1 damage to that player " +
            "or a planeswalker that player controls."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "138"
        artist = "E. M. Gist"
        flavorText = "\"May your veins weep, and may every vampire take notice.\"\n—Stensian curse"
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71010182-c004-4d18-adab-80319cd1e625.jpg?1783940940"
    }
}
