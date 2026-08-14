package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeAttackedWhileAttached
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * The Aetherspark — Aetherdrift #231
 * {4} · Legendary Artifact Planeswalker — Equipment · Loyalty 4
 */
val TheAetherspark = card("The Aetherspark") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Legendary Artifact Planeswalker — Equipment"
    startingLoyalty = 4
    oracleText = "As long as The Aetherspark is attached to a creature, The Aetherspark can't be " +
        "attacked and has \"Whenever equipped creature deals combat damage during your turn, put " +
        "that many loyalty counters on The Aetherspark.\"\n" +
        "+1: Attach The Aetherspark to up to one target creature you control. Put a +1/+1 counter " +
        "on that creature.\n" +
        "−5: Draw two cards.\n" +
        "−10: Add ten mana of any one color."

    staticAbility {
        ability = CantBeAttackedWhileAttached
    }

    triggeredAbility {
        trigger = Triggers.dealsDamage(
            damageType = DamageType.Combat,
            recipient = RecipientFilter.Any,
            binding = TriggerBinding.ATTACHED,
        )
        triggerCondition = Conditions.IsYourTurn
        effect = Effects.AddDynamicCounters(
            Counters.LOYALTY,
            DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT),
            EffectTarget.Self,
        )
        description = "Whenever equipped creature deals combat damage during your turn, put that " +
            "many loyalty counters on The Aetherspark."
    }

    loyaltyAbility(+1) {
        val creature = target(
            "up to one target creature you control",
            TargetCreature(optional = true, filter = TargetFilter.CreatureYouControl),
        )
        effect = Effects.AttachEquipment(creature) then
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    loyaltyAbility(-5) {
        effect = Effects.DrawCards(2)
    }

    loyaltyAbility(-10) {
        effect = Effects.AddManaOfChoice(amount = 10)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "231"
        artist = "Donato Giancola"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/05690d52-06c4-40b1-8360-380418a83250.jpg?1783907850"

        ruling(
            "2025-02-07",
            "If an effect causes The Aetherspark to become attached to a creature while it is being " +
                "attacked, it will continue to be attacked."
        )
    }
}
