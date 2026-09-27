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
 * Brutal Deceiver
 * {2}{R}
 * Creature — Spirit
 * 2/2
 *
 * {1}: Look at the top card of your library.
 * {2}: Reveal the top card of your library. If it's a land card, this creature gets +1/+0 and
 * gains first strike until end of turn. Activate only once each turn.
 *
 * Neither ability moves a card: both are gather-only pipelines over `TopOfLibrary(1)`, so the
 * card stays on top either way. The look is a private gather (the controller is the look
 * audience); the reveal is a public gather (`revealed = true`), and the land check reads that
 * revealed collection, so an empty library reveals nothing and grants nothing. "Activate only
 * once each turn" belongs to the second ability only, via [ActivationRestriction.OncePerTurn].
 */
val BrutalDeceiver = card("Brutal Deceiver") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 2
    oracleText = "{1}: Look at the top card of your library.\n" +
        "{2}: Reveal the top card of your library. If it's a land card, this creature gets +1/+0 " +
        "and gains first strike until end of turn. Activate only once each turn."

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = Effects.Pipeline {
            gather(CardSource.TopOfLibrary(1))
        }
        description = "{1}: Look at the top card of your library."
    }

    activatedAbility {
        cost = Costs.Mana("{2}")
        effect = Effects.Pipeline {
            val revealedTop = gather(CardSource.TopOfLibrary(1), revealed = true)
            run(Effects.If(
                condition = whenMatches(revealedTop, GameObjectFilter.Land),
                then = Effects.ModifyStats(1, 0, EffectTarget.Self) then
                    Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self),
            ))
        }
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        description = "{2}: Reveal the top card of your library. If it's a land card, this " +
            "creature gets +1/+0 and gains first strike until end of turn. Activate only once each turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "161"
        artist = "Jon Foster"
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e3d532b3-4bd6-4c1d-974d-789976117497.jpg?1783944302"
    }
}
