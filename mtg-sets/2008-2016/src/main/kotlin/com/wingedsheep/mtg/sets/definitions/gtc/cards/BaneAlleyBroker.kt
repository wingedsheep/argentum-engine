package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MayLookAtLinkedExile
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Bane Alley Broker — Gatecrash #145
 * {1}{U}{B} · Creature — Human Rogue 0/3
 *
 * {T}: Draw a card, then exile a card from your hand face down.
 * You may look at cards exiled with this creature.
 * {U}{B}, {T}: Return a card exiled with this creature to its owner's hand.
 *
 * Modeling notes:
 *  - The exile links to the Broker (`linkToSource`) and carries no `lookableInExile` stamp of its
 *    own: the look is the second ability, [MayLookAtLinkedExile], a static of the permanent. Its
 *    current controller may look at the pile, and the engine latches that look for as long as each
 *    card stays exiled (ruling: you keep looking after you lose control of the Broker).
 *  - The pile belongs to this object only. A Broker that leaves and returns is a new object with no
 *    link to the old pile (CR 400.7), so its third ability returns nothing.
 *  - "Its owner's hand": a hand move routes to the card's owner, so a card exiled while an opponent
 *    controlled the Broker goes back to that opponent.
 */
val BaneAlleyBroker = card("Bane Alley Broker") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Human Rogue"
    power = 0
    toughness = 3
    oracleText = "{T}: Draw a card, then exile a card from your hand face down.\n" +
        "You may look at cards exiled with this creature.\n" +
        "{U}{B}, {T}: Return a card exiled with this creature to its owner's hand."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.Pipeline {
            run(Effects.DrawCards(1))
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
            val chosen = chooseExactly(1, from = hand, prompt = "Choose a card to exile face down")
            move(chosen, CardDestination.ToZone(Zone.EXILE), faceDown = FaceDownMode.HIDDEN, linkToSource = true)
        }
        description = "Draw a card, then exile a card from your hand face down."
    }

    staticAbility {
        ability = MayLookAtLinkedExile
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U}{B}"), Costs.Tap)
        effect = Effects.Pipeline {
            val pile = gather(CardSource.FromLinkedExile())
            val chosen = chooseExactly(1, from = pile, prompt = "Choose a card to return to its owner's hand")
            move(chosen, CardDestination.ToZone(Zone.HAND))
        }
        description = "Return a card exiled with this creature to its owner's hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "145"
        artist = "Clint Cearley"
        imageUri = "https://cards.scryfall.io/normal/front/7/9/7996df7f-70f5-412c-9573-9512e4e131ac.jpg?1783940112"
        ruling("2013-01-24", "If Bane Alley Broker's first ability resolves when you have no cards in your hand, you'll draw a card and then exile it. You won't have the opportunity to cast that card (or do anything else with it) before exiling it.")
        ruling("2013-01-24", "Once you are allowed to look at a face-down card in exile, you are allowed to look at that card as long as it's exiled. If you no longer control Bane Alley Broker when its last ability resolves, you can continue to look at the relevant cards in exile to choose one to return.")
        ruling("2013-01-24", "Bane Alley Broker's second and third abilities apply to cards exiled with that specific Bane Alley Broker, not any other creature named Bane Alley Broker. You should keep cards exiled by different Bane Alley Brokers separate.")
        ruling("2013-01-24", "If Bane Alley Broker leaves the battlefield, the cards exiled with it will be exiled indefinitely. If it later returns to the battlefield, it will be a new object with no connection to the cards exiled with it in its previous existence. You won't be able to use the \"new\" Bane Alley Broker to return cards exiled with the \"old\" one.")
        ruling("2013-01-24", "Even if not all players can look at the exiled cards, each card's owner is still known. It is advisable to keep cards owned by different players in distinct piles in case another player gains control of Bane Alley Broker and exiles one or more cards with it.")
    }
}
