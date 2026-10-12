package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.namedFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Legions to Ashes
 * {1}{W}{B}
 * Sorcery
 * Exile target nonland permanent an opponent controls and all tokens that player controls with
 * the same name as that permanent.
 *
 * Resolution pipeline mirrors Sever the Bloodline's name-capture: gather the chosen target and
 * capture its name, then gather every token sharing that name controlled by the target's
 * controller ([EffectTarget.TargetController]) — both gathered while the target is still on the
 * battlefield — and exile them. The token gather excludes the target itself, so a token target is
 * moved exactly once. A nontoken target is exiled too; an illegal target fizzles the
 * whole spell (per the rulings), which the engine's target re-check already does.
 */
val LegionsToAshes = card("Legions to Ashes") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Sorcery"
    oracleText = "Exile target nonland permanent an opponent controls and all tokens that player " +
        "controls with the same name as that permanent."

    spell {
        target(TargetFilter.NonlandPermanentOpponentControls)
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            val chosenName = storeCardName(chosen)
            val sameNamedTokens = gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Token
                        .namedFromVariable(chosenName)
                        .targetPlayerControls(EffectTarget.TargetController),
                    excludeChosenTargets = true
                )
            )
            exile(chosen)
            exile(sameNamedTokens)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "215"
        artist = "Artur Nakhodkin"
        flavorText = "\"For the love of the seven brass gods, it's a city of academics! How are they doing this?\"\n—Sharaman, Urzan lieutenant"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/94faea39-4540-4adc-b9bd-78fefd09ecdf.jpg?1783920028"
        ruling("2022-10-14", "The target permanent need not be a token. In that case, it will still be exiled.")
        ruling("2022-10-14", "If the target is illegal as Legions to Ashes tries to resolve, it will be removed from the stack and do nothing, even if there are tokens with that name still on the battlefield.")
    }
}
