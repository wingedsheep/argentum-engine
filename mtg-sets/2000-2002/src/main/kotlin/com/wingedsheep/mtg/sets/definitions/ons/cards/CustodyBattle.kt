package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Custody Battle
 * {1}{R}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has "At the beginning of your upkeep, target opponent gains
 * control of this creature unless you sacrifice a land."
 *
 * The quoted ability is granted *to* the creature (Commander's Authority shape), so the creature's
 * controller controls the trigger: "your upkeep", the opponent and the land all key off whoever
 * holds the creature now, which is what makes the hot potato pass back and forth.
 */
val CustodyBattle = card("Custody Battle") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature has \"At the beginning of your upkeep, target opponent gains control of this creature unless you sacrifice a land.\""

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantTriggeredAbility(
            grantedTriggeredAbility {
                trigger = Triggers.you.beginningOf(Step.UPKEEP)
                val t = target(Targets.Opponent)
                effect = Effects.PayOrSuffer(
                    cost = Costs.pay.Sacrifice(GameObjectFilter.Land),
                    suffer = Effects.GiveControl(
                        permanent = EffectTarget.Self,
                        newController = t
                    )
                )
            }
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "197"
        artist = "Greg Hildebrandt & Tim Hildebrandt"
        flavorText = "Everyone wanted it. No one wanted to keep it."
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b72257f5-0cf9-45ca-8dc7-a1a93bd7dd1e.jpg?1562938173"
    }
}
