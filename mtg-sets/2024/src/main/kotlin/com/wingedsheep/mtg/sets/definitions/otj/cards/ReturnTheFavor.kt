package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

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
 * [Effects.ChangeTarget] + `TargetFilter.SpellOrAbilityOnStack.withSingleTarget()` (see
 * Willbender). "With a single target" is a targeting restriction, so a spell or ability with no
 * targets or several can't be chosen.
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
        effect = Effects.Modal(
            modes = listOf(
                // + {1} — Copy target instant/sorcery spell, activated ability, or triggered ability.
                mode("+ {1} — Copy target instant spell, sorcery spell, activated " +
                    "ability, or triggered ability. You may choose new targets for the copy.") {
                    val instantSorcerySpellOrAbility = target(TargetFilter.InstantSorcerySpellOrAbilityOnStack)
                    additionalManaCost = "{1}"
                    effect = Effects.CopyTargetSpellOrAbility(target = instantSorcerySpellOrAbility)
                },
                // + {1} — Change the target of target spell or ability with a single target.
                Mode(
                    effect = Effects.ChangeTarget(),
                    targetRequirements = listOf(TargetObject(filter = TargetFilter.SpellOrAbilityOnStack.withSingleTarget())),
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
