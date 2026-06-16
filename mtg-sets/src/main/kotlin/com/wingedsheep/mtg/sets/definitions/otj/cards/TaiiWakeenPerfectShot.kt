package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Taii Wakeen, Perfect Shot
 * {R}{W}
 * Legendary Creature — Human Mercenary
 * 2/3
 *
 * Whenever a source you control deals noncombat damage to a creature equal to that creature's
 * toughness, draw a card.
 *
 * {X}, {T}: If a source you control would deal noncombat damage to a permanent or player this turn,
 * it deals that much damage plus X instead.
 *
 * Implementation notes:
 * - Ability 1 is an observer trigger: `dealsDamage(NonCombat, AnyCreature, sourceFilter = "you
 *   control", binding = ANY)`. The intervening-if (CR 603.4) compares the damage dealt
 *   (`TRIGGER_DAMAGE_AMOUNT`) to the recipient creature's toughness as it last existed at damage
 *   time (`TRIGGER_RECIPIENT_TOUGHNESS`, captured as LKI so a lethal hit still reads the original
 *   toughness). EQ — so over-lethal damage does not draw a card.
 * - Ability 2 installs a turn-duration noncombat-damage amplification (CR 616) whose +X bonus is
 *   the X paid; it applies to every source the controller controls, hitting any permanent or
 *   player. Combat damage is unaffected.
 */
val TaiiWakeenPerfectShot = card("Taii Wakeen, Perfect Shot") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Human Mercenary"
    power = 2
    toughness = 3
    oracleText = "Whenever a source you control deals noncombat damage to a creature equal to that " +
        "creature's toughness, draw a card.\n" +
        "{X}, {T}: If a source you control would deal noncombat damage to a permanent or player " +
        "this turn, it deals that much damage plus X instead."

    triggeredAbility {
        trigger = Triggers.dealsDamage(
            damageType = DamageType.NonCombat,
            recipient = RecipientFilter.AnyCreature,
            sourceFilter = GameObjectFilter.Any.youControl(),
            binding = TriggerBinding.ANY,
        )
        // Intervening-if: the damage dealt equals the recipient creature's toughness (LKI).
        triggerCondition = Conditions.CompareAmounts(
            DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT),
            ComparisonOperator.EQ,
            DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_RECIPIENT_TOUGHNESS),
        )
        effect = Effects.DrawCards(1)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}"), Costs.Tap)
        effect = Effects.AmplifyNoncombatDamageThisTurn(DynamicAmount.XValue)
        description = "{X}, {T}: If a source you control would deal noncombat damage to a permanent " +
            "or player this turn, it deals that much damage plus X instead."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "234"
        artist = "David Auden Nash"
        flavorText = "\"You'd best keep yourself out of my sights.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/6/1643af0b-fcbf-4636-8c50-77ec77eaa34d.jpg?1712356219"
    }
}
