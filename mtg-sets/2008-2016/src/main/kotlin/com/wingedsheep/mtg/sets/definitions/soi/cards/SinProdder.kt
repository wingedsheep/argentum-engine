package com.wingedsheep.mtg.sets.definitions.soi.cards

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
 * Sin Prodder — Shadows over Innistrad #181
 * {2}{R} · Creature — Devil · 3/2 · Rare
 *
 * Menace
 * At the beginning of your upkeep, reveal the top card of your library. Any opponent may have you
 * put that card into your graveyard. If a player does, this creature deals damage to that player
 * equal to that card's mana value. Otherwise, put that card into your hand.
 *
 * A free-choice [Effects.AnyPlayerMayChoose] over the controller's opponents in turn order: the
 * first opponent to accept bins the card and takes its mana value from Sin Prodder, and nobody
 * after them is asked (the 2016-04-08 ruling). If every opponent declines, the card goes to hand.
 */
val SinProdder = card("Sin Prodder") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Devil"
    power = 3
    toughness = 2
    oracleText = "Menace\n" +
        "At the beginning of your upkeep, reveal the top card of your library. Any opponent may have " +
        "you put that card into your graveyard. If a player does, this creature deals damage to that " +
        "player equal to that card's mana value. Otherwise, put that card into your hand."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Pipeline {
            val revealed = gather(CardSource.TopOfLibrary(1, Player.You), revealed = true)
            // An empty library reveals nothing, so there is no card to offer.
            run(Effects.If(whenMatches(revealed), Effects.AnyPlayerMayChoose(
                prompt = "Put the revealed card into its owner's graveyard? If you do, Sin Prodder " +
                    "deals you damage equal to its mana value. If no one does, it goes to their hand.",
                consequence = Effects.Pipeline {
                    toGraveyard(revealed)
                    run(Effects.DealDamage(
                        amount = DynamicAmounts.manaValueOf(revealed),
                        target = EffectTarget.PlayerRef(Player.BoundVariable("prodded")),
                        damageSource = EffectTarget.Self,
                    ))
                },
                otherwise = Effects.Pipeline { toHand(revealed) },
                eligiblePlayers = Player.EachOpponent,
                chooserAs = "prodded",
            )))
        }
        description = "At the beginning of your upkeep, reveal the top card of your library. Any opponent " +
            "may have you put that card into your graveyard. If a player does, this creature deals damage " +
            "to that player equal to that card's mana value. Otherwise, put that card into your hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "181"
        artist = "Jack Wang"
        imageUri = "https://cards.scryfall.io/normal/front/c/e/cefeb031-9dda-4717-a8d1-e2c21551e43a.jpg?1783937742"
        ruling(
            "2016-04-08",
            "Each opponent in turn order, starting with the one after you in turn order, may choose to " +
                "have you put that card into your graveyard. Once a player does so, Sin Prodder deals " +
                "damage equal to that card's mana value to that player immediately and Sin Prodder's " +
                "trigger has no further action.",
        )
    }
}
