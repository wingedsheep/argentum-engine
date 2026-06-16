package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Highway Robbery
 * {1}{R}
 * Sorcery
 * You may discard a card or sacrifice a land. If you do, draw two cards.
 * Plot {1}{R}
 *
 * The optional "discard a card or sacrifice a land" fork is a `chooseCount = 1, minChooseCount = 0`
 * [ModalEffect] (the proven "choose up to one"/"you may" shape — Hullbreaker Horror). Declining
 * the modal is the "you may" no. Each mode gates its "draw two cards" payoff on the chosen cost
 * actually happening via [Effects.IfYouDo] (so picking "discard a card" with an empty hand, or
 * "sacrifice a land" with no lands, does not draw).
 *
 * Both cost halves are gather → select → move pipelines so [Effects.IfYouDo]'s default
 * `SuccessCriterion.Auto` can infer "did it happen" from the terminal zone move: discard moves the
 * chosen card to the graveyard, and sacrifice gathers the controller's lands, lets them pick one,
 * and moves it to the graveyard as a real [MoveType.Sacrifice]. With no lands to sacrifice the
 * select stores nothing, the move is empty, and Auto reports no success — so no draw. Plot is the
 * standard [KeywordAbility.plot] exile-and-cast-later mechanic.
 */
val HighwayRobbery = card("Highway Robbery") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "You may discard a card or sacrifice a land. If you do, draw two cards.\n" +
        "Plot {1}{R} (You may pay {1}{R} and exile this card from your hand. Cast it as a sorcery " +
        "on a later turn without paying its mana cost. Plot only as a sorcery.)"

    spell {
        effect = ModalEffect(
            modes = listOf(
                Mode(
                    effect = Effects.IfYouDo(
                        action = Effects.Discard(1, EffectTarget.Controller),
                        ifYouDo = Effects.DrawCards(2)
                    ),
                    description = "Discard a card, then draw two cards"
                ),
                Mode(
                    effect = Effects.IfYouDo(
                        action = Effects.Pipeline {
                            val lands = gather(GameObjectFilter.Land, player = Player.You)
                            val chosen = chooseExactly(
                                1,
                                from = lands,
                                useTargetingUI = true,
                                prompt = "Choose a land to sacrifice"
                            )
                            sacrifice(chosen)
                        },
                        ifYouDo = Effects.DrawCards(2)
                    ),
                    description = "Sacrifice a land, then draw two cards"
                )
            ),
            chooseCount = 1,
            minChooseCount = 0,
            countsAsModalSpell = false
        )
    }

    keywordAbility(KeywordAbility.plot("{1}{R}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "129"
        artist = "Scott Murphy"
        flavorText = "The fires weren't even out before the backstabbing began."
        imageUri = "https://cards.scryfall.io/normal/front/3/1/31a88429-9204-4a23-a7a8-babbd6bab79f.jpg?1712355775"
    }
}
