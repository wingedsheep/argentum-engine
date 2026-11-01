package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Deliver Unto Evil — War of the Spark #85 (canonical printing)
 * {2}{B}
 * Sorcery
 * Choose up to four target cards in your graveyard. If you control a Bolas planeswalker, return
 * those cards to your hand. Otherwise, an opponent chooses two of them. Leave the chosen cards in
 * your graveyard and put the rest into your hand.
 * Exile Deliver Unto Evil.
 *
 * Whether you control a Bolas planeswalker is checked on resolution ([Effects.If]). Both branches
 * gather the still-legal targets ([CardSource.ChosenTargets]); without Bolas an opponent
 * (`Chooser.Opponent`, the caster picks which one in multiplayer) chooses exactly two of them to
 * stay — with two or fewer targets they all stay — and the remainder goes to hand.
 * `selfExile()` exiles the resolved sorcery; a fizzled one goes to the graveyard (2019-05-03).
 */
val DeliverUntoEvil = card("Deliver Unto Evil") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose up to four target cards in your graveyard. If you control a Bolas planeswalker, " +
        "return those cards to your hand. Otherwise, an opponent chooses two of them. Leave the chosen " +
        "cards in your graveyard and put the rest into your hand.\nExile Deliver Unto Evil."

    spell {
        targets(TargetFilter.CardInGraveyard.ownedByYou(), count = 4, optional = true)
        effect = Effects.If(
            condition = Conditions.YouControl(GameObjectFilter.Planeswalker.withSubtype("Bolas")),
            then = Effects.Pipeline {
                toHand(gather(CardSource.ChosenTargets))
            },
            otherwise = Effects.Pipeline {
                val targeted = gather(CardSource.ChosenTargets)
                val (_, toHandCards) = chooseExactlySplit(
                    2,
                    from = targeted,
                    chooser = Chooser.Opponent,
                    selectedLabel = "Stay in graveyard",
                    remainderLabel = "Return to hand",
                    prompt = "Choose two of these cards to leave in their owner's graveyard; " +
                        "the rest go to their hand."
                )
                toHand(toHandCards)
            }
        )
        selfExile()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "85"
        artist = "Seb McKinnon"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67aa7104-7acc-4827-b763-ac053c99baab.jpg?1783933447"
        ruling("2019-05-03", "Whether you control a Bolas planeswalker is checked only as this spell resolves.")
        ruling(
            "2019-05-03",
            "If some of the targets chosen are no longer legal targets as Deliver Unto Evil resolves, an " +
                "opponent chooses among the targets that remain legal. If no targets remain legal, the spell " +
                "doesn't resolve. It's put into its owner's graveyard and isn't exiled."
        )
        ruling(
            "2019-05-03",
            "You may cast Deliver Unto Evil with fewer than four targets. However, if you don't control a " +
                "Bolas planeswalker as Deliver Unto Evil resolves, your opponent will choose two of however " +
                "many cards you did target (or one or zero, if you chose fewer than two) and the remainder " +
                "(if any) will be put into your hand."
        )
        ruling(
            "2019-05-03",
            "You may cast Deliver Unto Evil with no targets if you wish. It will be exiled, and nothing else happens."
        )
        ruling(
            "2019-05-03",
            "You choose one opponent to make the final choice of which cards remains in your graveyard, but " +
                "all players may discuss and advise this choice."
        )
    }
}
