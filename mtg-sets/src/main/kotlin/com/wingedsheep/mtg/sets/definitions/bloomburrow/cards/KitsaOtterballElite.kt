package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Kitsa, Otterball Elite
 * {1}{U}
 * Legendary Creature — Otter Wizard
 * 1/3
 *
 * Vigilance
 * Prowess
 * {T}: Draw a card, then discard a card.
 * {2}, {T}: Copy target instant or sorcery spell you control. You may choose new targets for
 * the copy. Activate only if Kitsa's power is 3 or greater.
 */
val KitsaOtterballElite = card("Kitsa, Otterball Elite") {
    manaCost = "{1}{U}"
    typeLine = "Legendary Creature — Otter Wizard"
    power = 1
    toughness = 3
    oracleText = "Vigilance\nProwess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)\n{T}: Draw a card, then discard a card.\n{2}, {T}: Copy target instant or sorcery spell you control. You may choose new targets for the copy. Activate only if Kitsa's power is 3 or greater."

    keywords(Keyword.VIGILANCE)
    prowess()

    // {T}: Draw a card, then discard a card.
    activatedAbility {
        cost = Costs.Tap
        effect = EffectPatterns.loot()
        description = "{T}: Draw a card, then discard a card."
    }

    // {2}, {T}: Copy target instant or sorcery spell you control.
    // Activate only if Kitsa's power is 3 or greater.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        val spell = target("spell", Targets.InstantOrSorcerySpellYouControl)
        effect = Effects.CopyTargetSpell(spell)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Compare(
                    left = DynamicAmount.EntityProperty(EntityReference.Source, EntityNumericProperty.Power),
                    operator = ComparisonOperator.GTE,
                    right = DynamicAmount.Fixed(3)
                )
            )
        )
        description = "{2}, {T}: Copy target instant or sorcery spell you control. You may choose new targets for the copy. Activate only if Kitsa's power is 3 or greater."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "54"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c8ff751a-ec64-41d5-b22c-2a483ad9a9b2.jpg?1721426117"
        ruling("2024-07-26", "The copy created by Kitsa's last ability is created on the stack, so it's not \"cast.\" Abilities that trigger when a player casts a spell won't trigger.")
        ruling("2024-07-26", "Once you've activated Kitsa's last ability, any changes to Kitsa's power won't stop the ability from resolving.")
    }
}
