package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStatsForCreatureGroup
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Caretaker's Talent
 * {2}{W}
 * Enchantment — Class
 *
 * (Gain the next level as a sorcery to add its ability.)
 * Whenever one or more tokens you control enter, draw a card.
 * This ability triggers only once each turn.
 *
 * {W}: Level 2
 * When this Class becomes level 2, create a token that's a copy of
 * target token you control.
 *
 * {3}{W}: Level 3
 * Creature tokens you control get +2/+2.
 */
val CaretakersTalent = card("Caretaker's Talent") {
    manaCost = "{2}{W}"
    typeLine = "Enchantment — Class"
    oracleText = "Whenever one or more tokens you control enter, draw a card. This ability triggers only once each turn.\n" +
        "{W}: Level 2 — When this Class becomes level 2, create a token that's a copy of target token you control.\n" +
        "{3}{W}: Level 3 — Creature tokens you control get +2/+2."

    // Level 1: Whenever one or more tokens you control enter, draw a card. Once per turn.
    triggeredAbility {
        trigger = Triggers.OneOrMorePermanentsEnter(GameObjectFilter.Token)
        oncePerTurn = true
        effect = Effects.DrawCards(1)
    }

    // Level 2: When this Class becomes level 2, create a token that's a copy of target token you control.
    classLevel(2, "{W}") {
        triggeredAbility {
            trigger = Triggers.EntersBattlefield
            val token = target(
                "token you control",
                TargetObject(filter = TargetFilter(GameObjectFilter.Token.youControl()))
            )
            effect = CreateTokenCopyOfTargetEffect(token)
        }
    }

    // Level 3: Creature tokens you control get +2/+2.
    classLevel(3, "{3}{W}") {
        staticAbility {
            ability = ModifyStatsForCreatureGroup(
                powerBonus = 2,
                toughnessBonus = 2,
                filter = GroupFilter(
                    (GameObjectFilter.Creature and GameObjectFilter.Token).youControl()
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "6"
        artist = "Lindsey Look"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad5ea98a-e36e-4ab9-b4da-cc572f3777db.jpg?1721425789"
        ruling("2024-07-26", "Each Class has five abilities. The three in the major sections of its text box are class abilities. Class abilities can be static, activated, or triggered abilities.")
        ruling("2024-07-26", "Gaining a level is a normal activated ability. It uses the stack and can be responded to.")
        ruling("2024-07-26", "You can't activate the first level ability of a Class unless that Class is level 1. You can't activate the second level ability of a Class unless that Class is level 2.")
        ruling("2024-07-26", "Gaining a level won't remove abilities that a Class had at a previous level.")
    }
}
