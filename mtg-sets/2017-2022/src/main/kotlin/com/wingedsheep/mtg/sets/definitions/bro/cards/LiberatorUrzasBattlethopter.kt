package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty

/**
 * Liberator, Urza's Battlethopter
 * {3}
 * Legendary Artifact Creature — Thopter
 * 1/2
 *
 * Flash
 * Flying
 * You may cast colorless spells and artifact spells as though they had flash.
 * Whenever you cast a spell, if the amount of mana spent to cast that spell is greater than
 * Liberator's power, put a +1/+1 counter on Liberator.
 *
 * The flash permission is Mai and Zuko's [GrantFlashToSpellType] over `colorless ∨ artifact`
 * spells, controller-only. The counter trigger is the power half of the increment keyword's
 * intervening-if: the triggering spell's actual mana spent compared against this creature's live
 * power (rechecked on resolution, CR 603.4).
 */
val LiberatorUrzasBattlethopter = card("Liberator, Urza's Battlethopter") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Legendary Artifact Creature — Thopter"
    power = 1
    toughness = 2
    oracleText = "Flash\nFlying\n" +
        "You may cast colorless spells and artifact spells as though they had flash.\n" +
        "Whenever you cast a spell, if the amount of mana spent to cast that spell is greater than Liberator's power, put a +1/+1 counter on Liberator."

    keywords(Keyword.FLASH, Keyword.FLYING)

    staticAbility {
        ability = GrantFlashToSpellType(
            filter = GameObjectFilter.Any.withCardPredicate(CardPredicate.IsColorless) or GameObjectFilter.Artifact,
            controllerOnly = true
        )
    }

    triggeredAbility {
        trigger = Triggers.you.casts()
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.propertyOf(EffectTarget.TriggeringEntity, EntityNumericProperty.ManaSpent),
            ComparisonOperator.GT,
            DynamicAmounts.sourcePower()
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever you cast a spell, if the amount of mana spent to cast that spell is greater than Liberator's power, put a +1/+1 counter on Liberator."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "237"
        artist = "Ekaterina Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c31e6768-bcab-43c4-bfc1-76e961689ae9.jpg?1783920018"
        ruling("2022-10-14", "The third ability applies only to casting spells. It does not, for example, change when you may activate abilities that can be activated \"only as a sorcery.\"")
        ruling("2022-10-14", "The last ability cares only about the amount of mana that was actually spent to cast the spell, regardless of its mana cost.")
    }
}
