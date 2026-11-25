package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Soul Tithe
 * {1}{W}
 * Enchantment — Aura
 * Enchant nonland permanent
 * At the beginning of the upkeep of enchanted permanent's controller, that player sacrifices it
 * unless they pay {X}, where X is its mana value.
 *
 * Erosion's trigger shape: an ATTACHED-bound upkeep trigger timed off the *enchanted permanent's*
 * controller, who is bound as the triggering player — so the tax falls on that player, not on the
 * Aura's controller. X is read at resolution off the enchanted permanent's mana value ({X} in its
 * cost counts as 0, a token's is 0 unless it copies a card), paid all-or-nothing as generic mana by
 * that player; an unaffordable X skips straight to the sacrifice.
 *
 * "That player sacrifices it": only the upkeep player can sacrifice it, so the sacrifice is gated on
 * them still controlling it (a control change in response leaves it alone), and performed by its
 * controller rather than by the Aura's controller who controls the ability.
 */
val SoulTithe = card("Soul Tithe") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant nonland permanent\n" +
        "At the beginning of the upkeep of enchanted permanent's controller, that player sacrifices " +
        "it unless they pay {X}, where X is its mana value."
    auraTarget = TargetObject(filter = TargetFilter.NonlandPermanent)

    triggeredAbility {
        trigger = Triggers.attached.beginningOf(Step.UPKEEP)
        effect = Effects.MayPay(
            cost = Effects.PayDynamicMana(
                amount = DynamicAmounts.manaValueOf(EffectTarget.EnchantedPermanent),
                payer = Player.TriggeringPlayer,
            ),
            then = Effects.Nothing,
            otherwise = Effects.If(
                condition = Conditions.EnchantedPermanentMatches(
                    GameObjectFilter.Permanent.controlledByTriggeringPlayer()
                ),
                then = Effects.SacrificeTarget(
                    EffectTarget.EnchantedPermanent,
                    sacrificedByItsController = true,
                ),
            ),
            decisionMaker = EffectTarget.PlayerRef(Player.TriggeringPlayer),
        )
        description = "At the beginning of the upkeep of enchanted permanent's controller, that " +
            "player sacrifices it unless they pay {X}, where X is its mana value."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "23"
        artist = "Dave Kendall"
        imageUri = "https://cards.scryfall.io/normal/front/7/7/77e546ce-f498-4136-9015-bb262c301716.jpg?1783940373"
        ruling(
            "2012-10-01",
            "The mana value of a creature token is 0, unless that token is a copy of another " +
                "creature, in which case it copies that creature's mana cost."
        )
        ruling("2012-10-01", "If the permanent has {X} in its mana cost, X is 0.")
    }
}
