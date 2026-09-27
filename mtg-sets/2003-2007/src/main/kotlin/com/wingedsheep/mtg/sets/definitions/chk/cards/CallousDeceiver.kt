package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Callous Deceiver
 * {2}{U}
 * Creature — Spirit
 * 1/3
 *
 * {1}: Look at the top card of your library.
 * {2}: Reveal the top card of your library. If it's a land card, this creature gets +1/+0 and
 * gains flying until end of turn. Activate only once each turn.
 *
 * The look is a lone non-destructive gather of the top card — the gather's default
 * `LookAudience.Controller` shows it to the activator only, and nothing moves it. The reveal is
 * the Iron Lad shape: a `revealed = true` gather leaves the card on top, and a
 * `whenMatches(..., Land)` gate runs the pump. An empty library reveals nothing, so no bonus.
 * "Activate only once each turn" belongs to the {2} ability alone.
 */
val CallousDeceiver = card("Callous Deceiver") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Spirit"
    power = 1
    toughness = 3
    oracleText = "{1}: Look at the top card of your library.\n" +
        "{2}: Reveal the top card of your library. If it's a land card, this creature gets " +
        "+1/+0 and gains flying until end of turn. Activate only once each turn."

    // {1}: Look at the top card of your library.
    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = Effects.Pipeline {
            gather(CardSource.TopOfLibrary(1))
        }
        description = "{1}: Look at the top card of your library."
    }

    // {2}: Reveal the top card of your library. If it's a land card, this creature gets +1/+0
    // and gains flying until end of turn. Activate only once each turn.
    activatedAbility {
        cost = Costs.Mana("{2}")
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        effect = Effects.Pipeline {
            val revealedTop = gather(CardSource.TopOfLibrary(1), revealed = true)
            run(Effects.If(
                condition = whenMatches(revealedTop, GameObjectFilter.Land),
                then = Effects.ModifyStats(1, 0, EffectTarget.Self) then
                    Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self),
            ))
        }
        description = "{2}: Reveal the top card of your library. If it's a land card, this " +
            "creature gets +1/+0 and gains flying until end of turn. Activate only once each turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "53"
        artist = "Kensuke Okabayashi"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/628d3058-d32e-438c-9a86-3e9c2be73c4a.jpg?1783944329"
    }
}
