package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jailbreak Scheme {U}
 * Sorcery
 *
 * Spree (Choose one or more additional costs.)
 * + {3} — Put a +1/+1 counter on target creature. It can't be blocked this turn.
 * + {2} — Target artifact or creature's owner puts it on their choice of the top or
 *         bottom of their library.
 *
 * Spree is modeled as a [ModalEffect] with `minChooseCount = 1`, `chooseCount = modes.size`,
 * and per-mode `additionalManaCost`. CR 700.2 / OTJ release notes: at least one mode must
 * be chosen; the same mode can't be chosen more than once (so `allowRepeat = false`).
 */
val JailbreakScheme = card("Jailbreak Scheme") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Spree (Choose one or more additional costs.)\n" +
        "+ {3} — Put a +1/+1 counter on target creature. It can't be blocked this turn.\n" +
        "+ {2} — Target artifact or creature's owner puts it on their choice of the top or bottom of their library."

    spell {
        effect = ModalEffect(
            modes = listOf(
                Mode(
                    effect = Effects.Composite(
                        listOf(
                            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0)),
                            Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.ContextTarget(0))
                        )
                    ),
                    targetRequirements = listOf(Targets.Creature),
                    description = "+ {3} — Put a +1/+1 counter on target creature. It can't be blocked this turn.",
                    additionalManaCost = "{3}"
                ),
                Mode(
                    effect = Effects.PutOnTopOrBottomOfLibrary(EffectTarget.ContextTarget(0)),
                    targetRequirements = listOf(Targets.CreatureOrArtifact),
                    description = "+ {2} — Target artifact or creature's owner puts it on their choice of the top or bottom of their library.",
                    additionalManaCost = "{2}"
                )
            ),
            chooseCount = 2,
            minChooseCount = 1
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "53"
        artist = "Inkognit"
        flavorText = "Satoru insisted he was just about to free himself."
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a4be8e47-9006-4770-8d99-68a684064a43.jpg?1712860592"

        ruling("2024-04-12", "Spells with spree have a + (plus sign) indicator in the upper right corner of the card frame. This has no rules meaning and serves only to remind players that at least one additional cost is required to cast the spell.")
        ruling("2024-04-12", "You must choose at least one of the listed modes and pay its associated additional cost in order to cast a spell with spree.")
        ruling("2024-04-12", "You choose the modes as you cast the spell with spree. Once modes are chosen, they can't be changed.")
        ruling("2024-04-12", "You can't choose the same mode more than once.")
        ruling("2024-04-12", "The mana value of a spell with spree is determined only by its mana cost. It doesn't matter which modes you choose or which additional costs you pay.")
        ruling("2024-04-12", "If an effect allows you to cast a spell with spree \"without paying its mana cost,\" you must still choose at least one mode and pay the associated additional costs.")
        ruling("2024-04-12", "The permanent's owner chooses whether to put it on the top or bottom of their library.")
    }
}
