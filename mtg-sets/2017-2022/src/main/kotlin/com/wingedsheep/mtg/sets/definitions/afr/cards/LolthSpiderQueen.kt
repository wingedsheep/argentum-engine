package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lolth, Spider Queen — Adventures in the Forgotten Realms #112 (canonical printing)
 * {3}{B}{B} · Legendary Planeswalker — Lolth · Loyalty 4
 *
 * Whenever a creature you control dies, put a loyalty counter on Lolth.
 * 0: You draw a card and you lose 1 life.
 * −3: Create two 2/1 black Spider creature tokens with reach and menace.
 * −8: You get an emblem with "Whenever an opponent is dealt combat damage by one or more creatures
 *     you control, if that player lost less than 8 life this turn, they lose life equal to the
 *     difference."
 *
 * The emblem is a permanent [Effects.CreateGlobalTriggeredAbility] holding the offensive
 * combat-damage batch trigger: it fires once per opponent hit, with that opponent as the triggering
 * player. The combat damage that caused the trigger already counts toward the life they lost this
 * turn, and both the intervening-if and the difference are read again on resolution — an opponent
 * dealt 3 combat damage loses 5 more, ending the turn at 8 life lost.
 */
val LolthSpiderQueen = card("Lolth, Spider Queen") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Planeswalker — Lolth"
    startingLoyalty = 4
    oracleText = "Whenever a creature you control dies, put a loyalty counter on Lolth.\n" +
        "0: You draw a card and you lose 1 life.\n" +
        "−3: Create two 2/1 black Spider creature tokens with reach and menace.\n" +
        "−8: You get an emblem with \"Whenever an opponent is dealt combat damage by one or more " +
        "creatures you control, if that player lost less than 8 life this turn, they lose life equal " +
        "to the difference.\""

    // Whenever a creature you control dies, put a loyalty counter on Lolth.
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dies()
        effect = Effects.AddCounters(CounterType.LOYALTY, 1, EffectTarget.Self)
    }

    // 0: You draw a card and you lose 1 life.
    loyaltyAbility(0) {
        effect = Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller)
    }

    // −3: Create two 2/1 black Spider creature tokens with reach and menace.
    loyaltyAbility(-3) {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Spider"),
            keywords = setOf(Keyword.REACH, Keyword.MENACE),
            count = 2
        )
    }

    // −8: You get an emblem with "Whenever an opponent is dealt combat damage by one or more
    //     creatures you control, if that player lost less than 8 life this turn, they lose life
    //     equal to the difference."
    loyaltyAbility(-8) {
        val lifeLost = DynamicAmounts.lifeLostThisTurn(Player.TriggeringPlayer)
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.oneOrMore(GameObjectFilter.Creature).dealCombatDamageToAPlayer()
                interveningIf = Conditions.CompareAmounts(lifeLost, ComparisonOperator.LT, 8)
                effect = Effects.LoseLife(8 - lifeLost, EffectTarget.PlayerRef(Player.TriggeringPlayer))
                description = LOLTH_EMBLEM_TEXT
            },
            descriptionOverride = LOLTH_EMBLEM_TEXT
        )
        description = "You get an emblem with \"$LOLTH_EMBLEM_TEXT\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "112"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/420a5689-d73e-4dda-82fd-8b8897ff5b55.jpg?1783926491"
    }
}

private const val LOLTH_EMBLEM_TEXT = "Whenever an opponent is dealt combat damage by one or more creatures you " +
    "control, if that player lost less than 8 life this turn, they lose life equal to the difference."
