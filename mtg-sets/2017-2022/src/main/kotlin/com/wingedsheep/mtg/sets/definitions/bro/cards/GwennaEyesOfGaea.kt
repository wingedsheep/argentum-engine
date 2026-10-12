package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gwenna, Eyes of Gaea
 * {2}{G}
 * Legendary Creature — Elf Druid Scout
 * 2/3
 * {T}: Add two mana in any combination of colors. Spend this mana only to cast creature spells or
 *      activate abilities of creature sources.
 * Whenever you cast a creature spell with power 5 or greater, put a +1/+1 counter on Gwenna and
 * untap it.
 *
 * The mana is Smokebraider's shape with Lukka's creature restriction:
 * [Effects.AddManaInAnyCombination] colours each pip independently, and
 * [ManaRestriction.CardTypeSpellsOrAbilitiesOnly] with `allowAbilities = true` admits both creature
 * spells and activated abilities of creature sources. The trigger is Monstrous Vortex's
 * power-5-or-greater cast trigger.
 */
val GwennaEyesOfGaea = card("Gwenna, Eyes of Gaea") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Elf Druid Scout"
    power = 2
    toughness = 3
    oracleText = "{T}: Add two mana in any combination of colors. Spend this mana only to cast " +
        "creature spells or activate abilities of creature sources.\n" +
        "Whenever you cast a creature spell with power 5 or greater, put a +1/+1 counter on Gwenna " +
        "and untap it."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaInAnyCombination(
            amount = 2,
            restriction = ManaRestriction.CardTypeSpellsOrAbilitiesOnly(
                cardType = CardType.CREATURE,
                allowAbilities = true,
            ),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Creature.powerAtLeast(5))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
            Effects.Untap(EffectTarget.Self)
        description = "Whenever you cast a creature spell with power 5 or greater, put a +1/+1 " +
            "counter on Gwenna and untap it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "185"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7ee387b7-18e4-41b7-aefe-f2b5954e3051.jpg?1783920043"
    }
}
