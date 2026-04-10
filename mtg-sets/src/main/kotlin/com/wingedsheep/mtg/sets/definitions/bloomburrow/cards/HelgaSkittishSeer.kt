package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameEvent.SpellCastEvent
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Helga, Skittish Seer
 * {G}{W}{U}
 * Legendary Creature — Frog Druid
 * 1/3
 *
 * Whenever you cast a creature spell with mana value 4 or greater, you draw a card,
 * gain 1 life, and put a +1/+1 counter on Helga.
 *
 * {T}: Add X mana of any one color, where X is Helga's power. Spend this mana only
 * to cast creature spells with mana value 4 or greater or creature spells with {X}
 * in their mana costs.
 *
 */
val HelgaSkittishSeer = card("Helga, Skittish Seer") {
    manaCost = "{G}{W}{U}"
    typeLine = "Legendary Creature — Frog Druid"
    power = 1
    toughness = 3
    oracleText = "Whenever you cast a creature spell with mana value 4 or greater, you draw a card, gain 1 life, and put a +1/+1 counter on Helga.\n{T}: Add X mana of any one color, where X is Helga's power. Spend this mana only to cast creature spells with mana value 4 or greater or creature spells with {X} in their mana costs."

    // Triggered ability: whenever you cast a creature spell with MV 4+
    triggeredAbility {
        trigger = TriggerSpec(
            SpellCastEvent(
                spellFilter = GameObjectFilter.Creature.manaValueAtLeast(4),
                player = Player.You
            ),
            TriggerBinding.ANY
        )
        effect = CompositeEffect(
            listOf(
                DrawCardsEffect(1),
                GainLifeEffect(1),
                AddCountersEffect(
                    counterType = Counters.PLUS_ONE_PLUS_ONE,
                    count = 1,
                    target = EffectTarget.Self
                )
            )
        )
    }

    // Mana ability: {T}: Add X mana of any one color, where X is Helga's power
    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddAnyColorMana(
            DynamicAmount.EntityProperty(
                EntityReference.Source,
                EntityNumericProperty.Power
            ),
            ManaRestriction.CreatureMV4OrXCost
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "217"
        artist = "Andrea Piparo"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/40339715-22d0-4f99-822b-a00d9824f27a.jpg?1721427078"
        ruling("2024-07-26", "If a spell has {X} in its mana cost, use the value chosen for that X to determine the mana value of that spell.")
        ruling("2024-07-26", "Helga's last ability is a mana ability. It doesn't use the stack and can't be responded to.")
        ruling("2024-07-26", "You don't have to spend all the mana added by Helga's last ability on the same spell.")
        ruling("2024-07-26", "If you spend the mana added by Helga's last ability on a creature spell with {X} in its mana cost, the mana can be spent on any part of that spell's total cost. You're not limited to spending it only on the {X} part.")
    }
}
