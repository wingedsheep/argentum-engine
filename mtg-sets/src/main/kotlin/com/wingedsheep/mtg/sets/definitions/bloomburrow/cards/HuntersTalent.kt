package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Hunter's Talent {G}
 * Enchantment — Class
 *
 * (Gain the next level as a sorcery to add its ability.)
 *
 * When this Class enters the battlefield, target creature you control deals damage
 * equal to its power to target creature you don't control.
 *
 * {1}{G}: Level 2
 * Whenever you attack, target attacking creature gets +1/+0 and gains trample
 * until end of turn.
 *
 * {3}{G}: Level 3
 * At the beginning of your end step, if you control a creature with power 4 or
 * greater, draw a card.
 */
val HuntersTalent = card("Hunter's Talent") {
    manaCost = "{1}{G}"
    typeLine = "Enchantment — Class"
    oracleText = "When this Class enters the battlefield, target creature you control deals damage equal to its power to target creature you don't control.\n{1}{G}: Level 2 — Whenever you attack, target attacking creature gets +1/+0 and gains trample until end of turn.\n{3}{G}: Level 3 — At the beginning of your end step, if you control a creature with power 4 or greater, draw a card."

    // Level 1: ETB — bite effect
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val myCreature = target("creature you control", Targets.CreatureYouControl)
        val theirCreature = target("creature you don't control", Targets.CreatureOpponentControls)
        effect = DealDamageEffect(
            amount = DynamicAmount.EntityProperty(
                EntityReference.Target(0),
                EntityNumericProperty.Power
            ),
            target = theirCreature,
            damageSource = myCreature
        )
    }

    // Level 2: Whenever you attack, target attacking creature gets +1/+0 and trample
    classLevel(2, "{1}{G}") {
        triggeredAbility {
            trigger = Triggers.YouAttack
            val attacker = target("attacking creature", Targets.AttackingCreature)
            effect = Effects.ModifyStats(1, 0, attacker)
                .then(Effects.GrantKeyword(Keyword.TRAMPLE, attacker))
        }
    }

    // Level 3: At the beginning of your end step, if you control a creature with power 4+, draw a card
    classLevel(3, "{3}{G}") {
        triggeredAbility {
            trigger = Triggers.YourEndStep
            triggerCondition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.powerAtLeast(4))
            effect = Effects.DrawCards(1)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "179"
        artist = "Kisung Koh"
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e9a31863-9649-4a4f-99e4-c93729938bd7.jpg?1739659559"
        ruling("2024-07-26", "If either target is illegal as the level 1 class ability resolves, no damage will be dealt.")
        ruling("2024-07-26", "Gaining a level is a normal activated ability. It uses the stack and can be responded to.")
        ruling("2024-07-26", "You can't activate the first level ability of a Class unless that Class is level 1. You can't activate the second level ability of a Class unless that Class is level 2.")
        ruling("2024-07-26", "Gaining a level won't remove abilities that a Class had at a previous level.")
    }
}
