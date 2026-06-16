package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect

/**
 * Return the Favor
 * {R}{R}
 * Instant
 *
 * Spree (Choose one or more additional costs.)
 * + {1} — Copy target instant spell, sorcery spell, activated ability, or triggered ability.
 *         You may choose new targets for the copy.
 * + {1} — Change the target of target spell or ability with a single target.
 *
 * Spree is a [ModalEffect] with `minChooseCount = 1`, `chooseCount = modes.size`, per-mode
 * `additionalManaCost`, and `allowRepeat = false` (CR 700.2 / OTJ release notes). Both modes
 * target.
 *
 * Mode 1 ("copy target spell or ability") uses [Effects.CopyTargetSpellOrAbility] +
 * [Targets.InstantSorcerySpellOrAbility]: one target requirement that admits an instant spell,
 * a sorcery spell, an activated ability, or a triggered ability, and the executor copies whichever
 * kind was chosen, prompting for new targets per CR 707.10c.
 *
 * Mode 2 ("change the target of target spell or ability with a single target") uses
 * [Effects.ChangeTarget] + [Targets.SpellOrAbilityWithSingleTarget] (see Willbender). The
 * single-target restriction is enforced at resolution by the change-target executor.
 */
val ReturnTheFavor = card("Return the Favor") {
    manaCost = "{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Spree (Choose one or more additional costs.)\n" +
        "+ {1} — Copy target instant spell, sorcery spell, activated ability, or triggered " +
        "ability. You may choose new targets for the copy.\n" +
        "+ {1} — Change the target of target spell or ability with a single target."

    spell {
        effect = ModalEffect(
            modes = listOf(
                // + {1} — Copy target instant/sorcery spell, activated ability, or triggered ability.
                Mode(
                    effect = Effects.CopyTargetSpellOrAbility(),
                    targetRequirements = listOf(Targets.InstantSorcerySpellOrAbility),
                    description = "+ {1} — Copy target instant spell, sorcery spell, activated " +
                        "ability, or triggered ability. You may choose new targets for the copy.",
                    additionalManaCost = "{1}"
                ),
                // + {1} — Change the target of target spell or ability with a single target.
                Mode(
                    effect = Effects.ChangeTarget(),
                    targetRequirements = listOf(Targets.SpellOrAbilityWithSingleTarget),
                    description = "+ {1} — Change the target of target spell or ability with a " +
                        "single target.",
                    additionalManaCost = "{1}"
                )
            ),
            chooseCount = 2,
            minChooseCount = 1
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "142"
        artist = "Eli Minaya"
        flavorText = "\"Don't you know it's rude not to share?\""
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a9cc02d1-799d-42aa-9bc2-4c05452b63b4.jpg?1712860602"
    }
}
