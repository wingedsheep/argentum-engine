package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Withercrown
 * {1}{B}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has base power 0 and has "At the beginning of your upkeep, you lose 1 life
 * unless you sacrifice this creature."
 *
 * Only power is set (layer 7b), so the creature keeps its toughness and counters or pumps still
 * raise its power. The quoted ability is granted *to* the creature, so its controller pays the
 * life — and "this creature" is the trigger's own source.
 */
val Withercrown = card("Withercrown") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature has base power 0 and has \"At the beginning of your upkeep, you lose 1 life unless you sacrifice this creature.\""

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = SetBasePowerToughnessStatic(power = 0, toughness = null)
    }

    staticAbility {
        ability = GrantTriggeredAbility(
            grantedTriggeredAbility {
                trigger = Triggers.you.beginningOf(Step.UPKEEP)
                effect = Effects.PayOrSuffer(
                    cost = Costs.pay.Sacrifice(GameObjectFilter.Creature.sourceItself()),
                    suffer = Effects.LoseLife(1, EffectTarget.Controller)
                )
            }
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "119"
        artist = "Miranda Meeks"
        flavorText = "\"We'll see each other again soon.\"\n—Egon, god of death"
        imageUri = "https://cards.scryfall.io/normal/front/5/9/597fe119-64a7-4499-8b69-738af5a71a20.jpg?1783928238"
    }
}
