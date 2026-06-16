package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
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
 * Wanted Griffin
 * {3}{W}
 * Creature — Griffin
 * 3/2
 *
 * Flying
 * When this creature dies, create a 1/1 red Mercenary creature token with "{T}: Target
 * creature you control gets +1/+0 until end of turn. Activate only as a sorcery."
 *
 * The created token is the standard OTJ Mercenary token (same one made by Prickly Pair /
 * Mourner's Surprise): a sorcery-speed [TimingRule.SorcerySpeed] tap ability that pumps a
 * creature you control +1/+0.
 */
val WantedGriffin = card("Wanted Griffin") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Griffin"
    power = 3
    toughness = 2
    oracleText = "Flying\nWhen this creature dies, create a 1/1 red Mercenary creature token with " +
        "\"{T}: Target creature you control gets +1/+0 until end of turn. Activate only as a sorcery.\""

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.Dies
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
        collectorNumber = "38"
        artist = "Alexandre Honoré"
        flavorText = "Many a would-be captor has gotten close enough to count its feathers, only to be " +
            "left behind with nothing but regret and a dangling rope."
        imageUri = "https://cards.scryfall.io/normal/front/6/2/624a176b-fe24-4441-8877-464cf172ccff.jpg?1712355379"
    }
}
