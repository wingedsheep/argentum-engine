package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.NoncombatDamageBonus
import com.wingedsheep.sdk.scripting.ReduceSpellCostByFilter
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Artist's Talent {1}{R}
 * Enchantment — Class
 *
 * (Gain the next level as a sorcery to add its ability.)
 *
 * Whenever you cast a noncreature spell, you may discard a card. If you do, draw a card.
 *
 * {2}{R}: Level 2
 * Noncreature spells you cast cost {1} less to cast.
 *
 * {2}{R}: Level 3
 * If a source you control would deal noncombat damage to an opponent or a permanent an
 * opponent controls, it deals that much damage plus 2 instead.
 */
val ArtistsTalent = card("Artist's Talent") {
    manaCost = "{1}{R}"
    typeLine = "Enchantment — Class"
    oracleText = "Whenever you cast a noncreature spell, you may discard a card. If you do, draw a card.\n{2}{R}: Level 2 — Noncreature spells you cast cost {1} less to cast.\n{2}{R}: Level 3 — If a source you control would deal noncombat damage to an opponent or a permanent an opponent controls, it deals that much damage plus 2 instead."

    // Level 1: Whenever you cast a noncreature spell, you may discard a card. If you do, draw a card.
    triggeredAbility {
        trigger = Triggers.YouCastNoncreature
        effect = MayEffect(
            CompositeEffect(
                listOf(
                    EffectPatterns.discardCards(1),
                    DrawCardsEffect(1, EffectTarget.Controller)
                )
            )
        )
    }

    // Level 2: Noncreature spells you cast cost {1} less to cast
    classLevel(2, "{2}{R}") {
        staticAbility {
            ability = ReduceSpellCostByFilter(GameObjectFilter.Companion.Noncreature, 1)
        }
    }

    // Level 3: Noncombat damage to opponents/their permanents gets +2
    classLevel(3, "{2}{R}") {
        staticAbility {
            ability = NoncombatDamageBonus(2)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "124"
        artist = "Lars Grant-West"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b9e51d9-189b-4dd6-87cb-628ea6373e81.jpg?1721426571"
        ruling("2024-07-26", "The cost reduction applies only to generic mana in the total cost of noncreature spells you cast.")
        ruling("2024-07-26", "The additional 2 damage is dealt by the same source as the original source of damage. The damage isn't dealt by Artist's Talent.")
        ruling("2024-07-26", "If another effect modifies how much damage a source would deal, including preventing some of it, the player being dealt damage or the controller of the permanent being dealt damage chooses an order in which to apply those effects.")
        ruling("2024-07-26", "Gaining a level is a normal activated ability. It uses the stack and can be responded to.")
        ruling("2024-07-26", "Gaining a level won't remove abilities that a Class had at a previous level.")
    }
}
