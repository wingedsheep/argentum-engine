package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Duskmantle Seer
 * {2}{U}{B}
 * Creature — Vampire Wizard
 * 4/4
 * Flying
 * At the beginning of your upkeep, each player reveals the top card of their library, loses life
 * equal to that card's mana value, then puts it into their hand.
 *
 * Dark Confidant's reveal-to-hand pipeline run once per player: [Effects.ForEachPlayer] rebinds
 * `Player.You` / [EffectTarget.Controller] to the player being processed.
 */
val DuskmantleSeer = card("Duskmantle Seer") {
    manaCost = "{2}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Vampire Wizard"
    power = 4
    toughness = 4
    oracleText = "Flying\nAt the beginning of your upkeep, each player reveals the top card of their library, " +
        "loses life equal to that card's mana value, then puts it into their hand."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.ForEachPlayer(
            Player.Each,
            Effects.Pipeline {
                val revealed = gather(CardSource.TopOfLibrary(1, Player.You), revealed = true)
                run(Effects.LoseLife(DynamicAmounts.manaValueOf(revealed), EffectTarget.Controller))
                toHand(revealed, revealed = true)
            }
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "159"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63711861-87e0-4a63-8b7b-f834aa5f3f18.jpg?1783940108"
    }
}
