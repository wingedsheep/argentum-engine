package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Prickly Pair
 * {2}{R}
 * Creature — Plant Mercenary
 * 2/2
 *
 * When this creature enters, create a 1/1 red Mercenary creature token with "{T}: Target
 * creature you control gets +1/+0 until end of turn. Activate only as a sorcery."
 *
 * The created token is the standard OTJ Mercenary token (same one made by Mourner's Surprise):
 * a sorcery-speed [TimingRule.SorcerySpeed] tap ability that pumps a creature you control +1/+0.
 */
val PricklyPair = card("Prickly Pair") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Plant Mercenary"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, create a 1/1 red Mercenary creature token with " +
        "\"{T}: Target creature you control gets +1/+0 until end of turn. Activate only as a sorcery.\""

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
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
                    timing = TimingRule.SorcerySpeed
                )
            ),
            imageUri = "https://cards.scryfall.io/normal/front/5/f/5f04607f-eed2-462e-897f-82e41e5f7049.jpg?1712316319"
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "137"
        artist = "Brian Valeza"
        flavorText = "They grew together, awoke together, and took up arms together. If they were going to fall, they'd do that together too."
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e70f2278-8857-46e4-aa2b-fff5589b750f.jpg?1712355809"
    }
}
