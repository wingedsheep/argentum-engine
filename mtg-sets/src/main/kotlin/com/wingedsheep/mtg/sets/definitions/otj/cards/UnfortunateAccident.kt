package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Unfortunate Accident {B}
 * Instant
 *
 * Spree (Choose one or more additional costs.)
 * + {2}{B} — Destroy target creature.
 * + {1} — Create a 1/1 red Mercenary creature token with "{T}: Target creature you
 *   control gets +1/+0 until end of turn. Activate only as a sorcery."
 *
 * Spree is modeled as a [ModalEffect] with `minChooseCount = 1`,
 * `chooseCount = modes.size`, and per-mode `additionalManaCost` (CR 702.166).
 * The token's granted tap-ability mirrors the standard OTJ Mercenary token
 * (cf. Hellspur Posse Boss): sorcery-speed `{T}` pump of a creature you control.
 */
val UnfortunateAccident = card("Unfortunate Accident") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Spree (Choose one or more additional costs.)\n" +
        "+ {2}{B} — Destroy target creature.\n" +
        "+ {1} — Create a 1/1 red Mercenary creature token with \"{T}: Target creature you " +
        "control gets +1/+0 until end of turn. Activate only as a sorcery.\""

    spell {
        effect = ModalEffect(
            modes = listOf(
                Mode(
                    effect = Effects.Destroy(EffectTarget.ContextTarget(0)),
                    targetRequirements = listOf(Targets.Creature),
                    description = "+ {2}{B} — Destroy target creature.",
                    additionalManaCost = "{2}{B}"
                ),
                Mode(
                    effect = CreateTokenEffect(
                        count = DynamicAmount.Fixed(1),
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
                    ),
                    description = "+ {1} — Create a 1/1 red Mercenary creature token with \"{T}: " +
                        "Target creature you control gets +1/+0 until end of turn. Activate only as a sorcery.\"",
                    additionalManaCost = "{1}"
                )
            ),
            chooseCount = 2,
            minChooseCount = 1
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "111"
        artist = "Josiah \"Jo\" Cameron"
        flavorText = "\"Looks like you've got a train to catch.\"\n—Laughing Jasper Flint"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c5a25d9-926e-4004-8830-2b2bf7bc0775.jpg?1712860615"

        ruling("2024-04-12", "You must choose at least one of the listed modes and pay its associated additional cost in order to cast a spell with spree.")
        ruling("2024-04-12", "You choose the modes as you cast the spell with spree. Once modes are chosen, they can't be changed.")
        ruling("2024-04-12", "You can't choose the same mode more than once.")
    }
}
