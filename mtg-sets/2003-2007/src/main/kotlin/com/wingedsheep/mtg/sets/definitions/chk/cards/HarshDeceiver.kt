package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Harsh Deceiver
 * {3}{W}
 * Creature — Spirit
 * 1/4
 * {1}: Look at the top card of your library.
 * {2}: Reveal the top card of your library. If it's a land card, untap this creature and it gets
 * +1/+1 until end of turn. Activate only once each turn.
 *
 * Same shape as its cycle-mate Feral Deceiver: both abilities are a non-destructive pipeline gather
 * of the top card, so the card stays on top. The `{1}` ability is a private look (default
 * `LookAudience.Controller`); the `{2}` ability reveals to every player (`revealed = true`) and gates
 * the untap-and-pump on [GameObjectFilter.Land]. An empty library reveals nothing, so the condition
 * is false. The "once each turn" cap binds only the `{2}` ability.
 */
val HarshDeceiver = card("Harsh Deceiver") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Spirit"
    power = 1
    toughness = 4
    oracleText = "{1}: Look at the top card of your library.\n" +
        "{2}: Reveal the top card of your library. If it's a land card, untap this creature and it " +
        "gets +1/+1 until end of turn. Activate only once each turn."

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
            val harshDeceiverRevealed = gather(CardSource.TopOfLibrary(1), revealed = true)
            run(Effects.If(
                condition = whenMatches(harshDeceiverRevealed, GameObjectFilter.Land),
                then = Effects.Untap(EffectTarget.Self) then
                    Effects.ModifyStats(1, 1, EffectTarget.Self),
            ))
        }
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        description = "{2}: Reveal the top card of your library. If it's a land card, untap this " +
            "creature and it gets +1/+1 until end of turn. Activate only once each turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "11"
        artist = "Heather Hudson"
        imageUri = "https://cards.scryfall.io/normal/front/0/1/01e19753-b94b-458f-a51b-c8ec8fbec6c8.jpg?1783944341"
    }
}
