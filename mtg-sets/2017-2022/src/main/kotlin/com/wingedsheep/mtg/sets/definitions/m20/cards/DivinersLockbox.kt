package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.namedFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Diviner's Lockbox (M20 #225)
 * {4}  Artifact
 *
 * {1}, {T}: Choose a card name, then reveal the top card of your library. If that card has the
 * chosen name, sacrifice this artifact and draw three cards. Activate only as a sorcery.
 *
 * Petra Sphinx's name-then-reveal pipeline, aimed at your own library: the revealed card stays on
 * top either way. On a match the Lockbox is sacrificed and you draw three — the draw doesn't depend
 * on the sacrifice, so it still happens if the Lockbox has already left the battlefield.
 */
val DivinersLockbox = card("Diviner's Lockbox") {
    manaCost = "{4}"
    typeLine = "Artifact"
    oracleText = "{1}, {T}: Choose a card name, then reveal the top card of your library. If that card has " +
        "the chosen name, sacrifice this artifact and draw three cards. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.Pipeline {
            val chosenName = chooseCardName()
            val revealed = gather(CardSource.TopOfLibrary(1, Player.You), revealed = true)
            ifThen(whenMatches(revealed, GameObjectFilter.Any.namedFromVariable(chosenName))) {
                run(Effects.SacrificeTarget(EffectTarget.Self))
                run(Effects.DrawCards(3))
            }
        }
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "225"
        artist = "Lake Hurwitz"
        flavorText = "\"I see it taunting me whenever I close my eyes.\"\n—Nayrex Slipfinger, master thief"
        imageUri = "https://cards.scryfall.io/normal/front/d/a/da55c6a1-1f26-4254-ac62-fdbe94278de5.jpg?1783932944"
    }
}
