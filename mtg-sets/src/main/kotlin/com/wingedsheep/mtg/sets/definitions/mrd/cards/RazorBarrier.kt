package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.EffectChoice

/**
 * Razor Barrier — Mirrodin #17
 * {1}{W} · Instant
 *
 * Target permanent you control gains protection from artifacts or from the color of your choice
 * until end of turn.
 *
 * Not a modal spell — there is no printed "Choose one —", so the "artifacts or a color" pick is
 * made **on resolution** (CR 608.2d — choices an effect offers that weren't made as part of
 * casting are announced while applying the effect), not as the spell is cast. That is exactly
 * [Effects.ChooseAction]: the controller picks a labeled branch when the spell resolves. The
 * colour branch nests the existing `ChooseColorThen` → `GrantProtectionFromChosenColor` pair, so
 * the second prompt only appears once "a color" is actually chosen.
 *
 * The target is a *permanent*, not a creature — protecting an artifact from artifacts (or a land
 * from red) is a legitimate use, and the keyword grant applies to any battlefield permanent.
 */
val RazorBarrier = card("Razor Barrier") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target permanent you control gains protection from artifacts or from the color " +
        "of your choice until end of turn."

    spell {
        val permanent = target("target permanent you control", Targets.PermanentYouControl)
        effect = Effects.ChooseAction(
            listOf(
                EffectChoice(
                    label = "Protection from artifacts",
                    effect = Effects.GrantProtectionFromCardType(CardType.ARTIFACT, permanent)
                ),
                EffectChoice(
                    label = "Protection from the color of your choice",
                    effect = Effects.ChooseColorThen(
                        Effects.GrantProtectionFromChosenColor(permanent)
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "17"
        artist = "Ron Spencer"
        flavorText = "\"We protect our homelands. Why should they not protect us?\""
        imageUri = "https://cards.scryfall.io/normal/front/6/7/6743fab2-75b4-4eb8-b416-b5f052473393.jpg?1783944560"
    }
}
