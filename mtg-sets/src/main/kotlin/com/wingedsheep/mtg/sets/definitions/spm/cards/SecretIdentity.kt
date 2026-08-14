package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Secret Identity
 * {U}
 * Instant
 *
 * Choose one —
 * • Conceal — Until end of turn, target creature you control becomes a Citizen with base power
 *   and toughness 1/1 and gains hexproof.
 * • Reveal — Until end of turn, target creature you control becomes a Hero with base power and
 *   toughness 3/4 and gains flying and vigilance.
 *
 * Each mode is the one-shot, end-of-turn counterpart of Spider-Man No More's become-creature
 * static stack: it replaces the creature's subtypes ([Effects.SetCreatureSubtypes], Layer 4),
 * sets its base P/T ([Effects.SetBasePowerAndToughness], Layer 7b), and grants keyword(s)
 * ([Effects.GrantKeyword], Layer 6) — all with [Duration.EndOfTurn] so the whole transform
 * reverts at cleanup. The card type stays CREATURE (subtypes are merely replaced) and colors are
 * left unchanged, matching the oracle wording ("becomes a Citizen/Hero", not "becomes a blue …").
 */
val SecretIdentity = card("Secret Identity") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Conceal — Until end of turn, target creature you control becomes a Citizen with base " +
        "power and toughness 1/1 and gains hexproof.\n" +
        "• Reveal — Until end of turn, target creature you control becomes a Hero with base power " +
        "and toughness 3/4 and gains flying and vigilance."

    spell {
        effect = ModalEffect(
            modes = listOf(
                Mode.withTarget(
                    effect = Effects.Composite(
                        Effects.SetCreatureSubtypes(
                            subtypes = setOf("Citizen"),
                            target = EffectTarget.ContextTarget(0),
                            duration = Duration.EndOfTurn
                        ),
                        Effects.SetBasePowerAndToughness(
                            power = 1,
                            toughness = 1,
                            target = EffectTarget.ContextTarget(0),
                            duration = Duration.EndOfTurn
                        ),
                        Effects.GrantKeyword(
                            keyword = Keyword.HEXPROOF,
                            target = EffectTarget.ContextTarget(0),
                            duration = Duration.EndOfTurn
                        )
                    ),
                    target = Targets.CreatureYouControl,
                    description = "Conceal — Until end of turn, target creature you control " +
                        "becomes a Citizen with base power and toughness 1/1 and gains hexproof."
                ),
                Mode.withTarget(
                    effect = Effects.Composite(
                        Effects.SetCreatureSubtypes(
                            subtypes = setOf("Hero"),
                            target = EffectTarget.ContextTarget(0),
                            duration = Duration.EndOfTurn
                        ),
                        Effects.SetBasePowerAndToughness(
                            power = 3,
                            toughness = 4,
                            target = EffectTarget.ContextTarget(0),
                            duration = Duration.EndOfTurn
                        ),
                        Effects.GrantKeyword(
                            keyword = Keyword.FLYING,
                            target = EffectTarget.ContextTarget(0),
                            duration = Duration.EndOfTurn
                        ),
                        Effects.GrantKeyword(
                            keyword = Keyword.VIGILANCE,
                            target = EffectTarget.ContextTarget(0),
                            duration = Duration.EndOfTurn
                        )
                    ),
                    target = Targets.CreatureYouControl,
                    description = "Reveal — Until end of turn, target creature you control becomes " +
                        "a Hero with base power and toughness 3/4 and gains flying and vigilance."
                )
            ),
            chooseCount = 1,
            minChooseCount = 1
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "43"
        artist = "rk post"
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37a31d84-e87b-406e-9249-fae1b5e23e72.jpg?1783905350"
    }
}
