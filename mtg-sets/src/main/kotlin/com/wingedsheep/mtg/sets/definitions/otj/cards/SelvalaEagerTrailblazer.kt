package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.CardNumericProperty
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.ManaColorSet

/**
 * Selvala, Eager Trailblazer
 * {2}{G}{W}
 * Legendary Creature — Elf Scout
 * 4/5
 *
 * Vigilance
 * Whenever you cast a creature spell, create a 1/1 red Mercenary creature token with "{T}: Target
 * creature you control gets +1/+0 until end of turn. Activate only as a sorcery."
 * {T}: Choose a color. Add one mana of that color for each different power among creatures you
 * control.
 *
 * The mana ability uses the new [Aggregation.DISTINCT_VALUES] over [CardNumericProperty.POWER]
 * ("number of different powers among creatures you control") feeding [Effects.AddManaOfChoice].
 * The Mercenary token mirrors Hellspur Posse Boss's token (same OTJ Mercenary print).
 */
val SelvalaEagerTrailblazer = card("Selvala, Eager Trailblazer") {
    manaCost = "{2}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Elf Scout"
    power = 4
    toughness = 5
    oracleText = "Vigilance\n" +
        "Whenever you cast a creature spell, create a 1/1 red Mercenary creature token with " +
        "\"{T}: Target creature you control gets +1/+0 until end of turn. Activate only as a " +
        "sorcery.\"\n" +
        "{T}: Choose a color. Add one mana of that color for each different power among creatures " +
        "you control."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.YouCastCreature
        effect = CreateTokenEffect(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Mercenary"),
            activatedAbilities = listOf(
                ActivatedAbility(
                    cost = AbilityCost.Tap,
                    effect = Effects.ModifyStats(1, 0, EffectTarget.ContextTarget(0)),
                    targetRequirements = listOf(Targets.CreatureYouControl),
                    timing = TimingRule.SorcerySpeed,
                )
            ),
            imageUri = "https://cards.scryfall.io/normal/front/5/f/5f04607f-eed2-462e-897f-82e41e5f7049.jpg?1712316319",
        )
        description = "Whenever you cast a creature spell, create a 1/1 red Mercenary creature " +
            "token with \"{T}: Target creature you control gets +1/+0 until end of turn. Activate " +
            "only as a sorcery.\""
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChoice(
            colorSet = ManaColorSet.AnyColor,
            amount = DynamicAmount.AggregateBattlefield(
                player = Player.You,
                filter = GameObjectFilter.Creature,
                aggregation = Aggregation.DISTINCT_VALUES,
                property = CardNumericProperty.POWER,
            ),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "Choose a color. Add one mana of that color for each different power among " +
            "creatures you control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "231"
        artist = "Viko Menezes"
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d2e167f-7cb2-4f15-a1db-7ee56b7ba523.jpg?1712356210"

        ruling("2024-04-12", "The number of different powers is calculated as the ability resolves. " +
            "For example, if you control three creatures with powers 1, 1, and 3, there are two " +
            "different powers, so you add two mana.")
        ruling("2024-04-12", "If you control no creatures (which can't normally happen, as Selvala " +
            "is a creature), or if all your creatures have the same power, you still choose a color.")
    }
}
