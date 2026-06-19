package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.GrantTriggeredAbilityEffect
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Great Hall of the Biblioplex
 * Land
 *
 * {T}: Add {C}.
 * {T}, Pay 1 life: Add one mana of any color. Spend this mana only to cast an instant or
 *   sorcery spell.
 * {5}: If this land isn't a creature, it becomes a 2/4 Wizard creature with "Whenever you
 *   cast an instant or sorcery spell, this creature gets +1/+0 until end of turn." It's
 *   still a land.
 *
 * The {5} animation is permanent (no "until end of turn" — the land stays a creature) and is
 * gated on the land not already being a creature, so re-activation is a no-op rather than
 * re-stacking the granted ability. The granted self-trigger and the become-creature effect
 * both use [Duration.Permanent]. Same man-land shape as Raging Ravine, but permanent and
 * intervening-if-gated.
 */
val GreatHallOfTheBiblioplex = card("Great Hall of the Biblioplex") {
    typeLine = "Land"
    colorIdentity = ""
    oracleText = "{T}: Add {C}.\n" +
        "{T}, Pay 1 life: Add one mana of any color. Spend this mana only to cast an instant or sorcery spell.\n" +
        "{5}: If this land isn't a creature, it becomes a 2/4 Wizard creature with \"Whenever you cast an " +
        "instant or sorcery spell, this creature gets +1/+0 until end of turn.\" It's still a land."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayLife(1))
        effect = Effects.AddManaOfChoice(restriction = ManaRestriction.InstantOrSorceryOnly)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{5}")
        effect = ConditionalEffect(
            condition = Conditions.Not(Conditions.SourceMatches(GameObjectFilter.Creature)),
            effect = Effects.Composite(
                BecomeCreatureEffect(
                    target = EffectTarget.Self,
                    power = DynamicAmount.Fixed(2),
                    toughness = DynamicAmount.Fixed(4),
                    creatureTypes = setOf("Wizard"),
                    duration = Duration.Permanent,
                ),
                GrantTriggeredAbilityEffect(
                    ability = TriggeredAbility.create(
                        trigger = Triggers.YouCastInstantOrSorcery.event,
                        binding = TriggerBinding.SELF,
                        effect = Effects.ModifyStats(power = 1, toughness = 0, target = EffectTarget.Self),
                        descriptionOverride = "Whenever you cast an instant or sorcery spell, this creature gets +1/+0 until end of turn.",
                    ),
                    target = EffectTarget.Self,
                    duration = Duration.Permanent,
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "257"
        artist = "Constantin Marin"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42d92674-2664-411c-b9c5-b04da7c845f4.jpg?1775938794"
    }
}
