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
 * Feral Deceiver
 * {3}{G}
 * Creature — Spirit
 * 3/2
 * {1}: Look at the top card of your library.
 * {2}: Reveal the top card of your library. If it's a land card, this creature gets +2/+2 and
 * gains trample until end of turn. Activate only once each turn.
 *
 * Both abilities are a non-destructive pipeline gather of the top card — the card is never moved,
 * so it stays on top. The `{1}` ability is a private look (the default `LookAudience.Controller`
 * audience); the `{2}` ability reveals it to every player (`revealed = true`) and gates the
 * pump-and-trample on [GameObjectFilter.Land] (Iron Lad, Diverging Destiny shape). An empty
 * library reveals nothing, so the condition is false and there is no pump. The "once each turn"
 * cap binds only the `{2}` ability.
 */
val FeralDeceiver = card("Feral Deceiver") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spirit"
    power = 3
    toughness = 2
    oracleText = "{1}: Look at the top card of your library.\n" +
        "{2}: Reveal the top card of your library. If it's a land card, this creature gets +2/+2 " +
        "and gains trample until end of turn. Activate only once each turn."

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
            val feralDeceiverRevealed = gather(CardSource.TopOfLibrary(1), revealed = true)
            run(Effects.If(
                condition = whenMatches(feralDeceiverRevealed, GameObjectFilter.Land),
                then = Effects.ModifyStats(2, 2, EffectTarget.Self) then
                    Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.Self),
            ))
        }
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        description = "{2}: Reveal the top card of your library. If it's a land card, this creature " +
            "gets +2/+2 and gains trample until end of turn. Activate only once each turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "208"
        artist = "Glen Angus"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c49d705-9b0c-4c2e-9c27-46eec35deb92.jpg?1783944291"
    }
}
