package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.GrantTriggeredAbilityEffect
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Vigorous Charge
 * {G}
 * Instant
 * Kicker {W}
 * Target creature gains trample until end of turn. Whenever that creature deals
 * combat damage this turn, if this spell was kicked, you gain life equal to that damage.
 */
val VigorousCharge = card("Vigorous Charge") {
    manaCost = "{G}"
    colorIdentity = "GW"
    typeLine = "Instant"
    oracleText = "Kicker {W} (You may pay an additional {W} as you cast this spell.)\n" +
        "Target creature gains trample until end of turn. Whenever that creature deals combat damage this turn, " +
        "if this spell was kicked, you gain life equal to that damage."

    keywordAbility(KeywordAbility.kicker("{W}"))

    spell {
        val t = target("target", Targets.Creature)
        effect = Effects.Composite(
            listOf(
                Effects.GrantKeyword(Keyword.TRAMPLE, t),
                ConditionalEffect(
                    condition = WasKicked,
                    effect = GrantTriggeredAbilityEffect(
                        ability = TriggeredAbility.create(
                            trigger = Triggers.dealsDamage(damageType = DamageType.Combat).event,
                            binding = Triggers.dealsDamage(damageType = DamageType.Combat).binding,
                            effect = Effects.GainLife(
                                DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT)
                            ),
                            descriptionOverride = "Whenever this creature deals combat damage, you gain life equal to that damage."
                        ),
                        target = t
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "222"
        artist = "Scott M. Fischer"
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af6f57ad-d370-4c81-8da0-c15d87725ab1.jpg?1562930306"
    }
}
