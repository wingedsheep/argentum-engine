package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Uro, Titan of Nature's Wrath
 * {1}{G}{U}
 * Legendary Creature — Elder Giant
 * 6/6
 * When Uro enters, sacrifice it unless it escaped.
 * Whenever Uro enters or attacks, you gain 3 life and draw a card, then you may put a land card
 * from your hand onto the battlefield.
 * Escape—{G}{G}{U}{U}, Exile five other cards from your graveyard.
 *
 * The titan shape of Kroxa and Phlage: the sacrifice is an [Effects.If] over [Conditions.Escaped],
 * checked as the trigger resolves, and the value trigger fires on entry either way. The land drop
 * is Eureka Moment's [Patterns.Hand.putFromHand] — an up-to-one selection, so the "you may" — which
 * is not playing a land and ignores land drops and timing (ruling).
 */
private val gainDrawRamp: Effect =
    Effects.GainLife(3, EffectTarget.Controller) then
        Effects.DrawCards(1) then
        Patterns.Hand.putFromHand(filter = GameObjectFilter.Land)

val UroTitanOfNaturesWrath = card("Uro, Titan of Nature's Wrath") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Elder Giant"
    power = 6
    toughness = 6
    oracleText = "When Uro enters, sacrifice it unless it escaped.\n" +
        "Whenever Uro enters or attacks, you gain 3 life and draw a card, then you may put a land card " +
        "from your hand onto the battlefield.\n" +
        "Escape—{G}{G}{U}{U}, Exile five other cards from your graveyard. " +
        "(You may cast this card from your graveyard for its escape cost.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.If(Conditions.Not(Conditions.Escaped), SacrificeSelfEffect)
        description = "When Uro enters, sacrifice it unless it escaped."
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = gainDrawRamp
        description = "Whenever Uro enters, you gain 3 life and draw a card, then you may put a land card " +
            "from your hand onto the battlefield."
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = gainDrawRamp
        description = "Whenever Uro attacks, you gain 3 life and draw a card, then you may put a land card " +
            "from your hand onto the battlefield."
    }

    keywordAbility(KeywordAbility.escape("{G}{G}{U}{U}", Costs.additional.ExileOtherCards(5)))

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "229"
        artist = "Vincent Proce"
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a0b6a71e-56cb-4d25-8f2b-7a4f1b60900d.jpg?1783931516"
        ruling(
            "2020-01-24",
            "Uro's first ability causes you to sacrifice it if you didn't cast it, or if it was cast using " +
                "any permission other than an escape ability."
        )
        ruling("2020-01-24", "Uro's second ability triggers when it enters the battlefield, even if it didn't escape.")
        ruling(
            "2020-01-24",
            "Uro's effect doesn't count as playing a land. It can put a land card onto the battlefield even if " +
                "it's not your turn or if you've already played your land for the turn."
        )
    }
}
