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
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Prosperity Tycoon
 * {3}{W}
 * Creature — Human Noble
 * 4/2
 *
 * When this creature enters, create a 1/1 red Mercenary creature token with "{T}: Target
 * creature you control gets +1/+0 until end of turn. Activate only as a sorcery."
 * {2}, Sacrifice a token: This creature gains indestructible until end of turn. Tap it.
 *
 * The created token is the standard OTJ Mercenary token (same one made by Prickly Pair /
 * Mourner's Surprise).
 */
val ProsperityTycoon = card("Prosperity Tycoon") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Noble"
    power = 4
    toughness = 2
    oracleText = "When this creature enters, create a 1/1 red Mercenary creature token with " +
        "\"{T}: Target creature you control gets +1/+0 until end of turn. Activate only as a sorcery.\"\n" +
        "{2}, Sacrifice a token: This creature gains indestructible until end of turn. Tap it."

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

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Sacrifice(GameObjectFilter.Token))
        effect = Effects.Composite(
            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self),
            Effects.Tap(EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "25"
        artist = "Caio Monteiro"
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f87824f3-aa9f-4d3d-99f7-0fbca43d8a51.jpg?1712355328"
    }
}
