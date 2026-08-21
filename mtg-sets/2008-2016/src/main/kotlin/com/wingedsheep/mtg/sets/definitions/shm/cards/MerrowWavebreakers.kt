package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Merrow Wavebreakers
 * {4}{U}
 * Creature — Merfolk Soldier
 * 3 / 3
 *
 * {1}{U}, {Q}: This creature gains flying until end of turn. ({Q} is the untap symbol.)
 *
 * - `{Q}` is [Costs.Untap]: the Wavebreakers must already be **tapped** to pay it, and CR 302.6
 *   gates the untap symbol behind summoning sickness exactly like `{T}`.
 * - The grant targets [EffectTarget.Self] rather than a chosen target — "this creature" is the
 *   source, so the ability has no target requirement at all.
 * - [Effects.GrantKeyword] defaults to `Duration.EndOfTurn`, which is the printed duration; nothing
 *   is passed explicitly.
 */
val MerrowWavebreakers = card("Merrow Wavebreakers") {
    manaCost = "{4}{U}"
    typeLine = "Creature — Merfolk Soldier"
    power = 3
    toughness = 3
    oracleText = "{1}{U}, {Q}: This creature gains flying until end of turn. ({Q} is the untap symbol.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{U}"), Costs.Untap)
        effect = Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self)
        description = "{1}{U}, {Q}: This creature gains flying until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "44"
        artist = "Alex Horley-Orlandelli"
        flavorText = "The merrows' prey have retreated from the shore, so they have learned to follow."
        imageUri = "https://cards.scryfall.io/normal/front/9/9/9919382e-3dcd-4f83-8135-be71345e57c0.jpg?1783942760"
    }
}
