package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Pelakka Predation {2}{B} // Pelakka Caverns — Zendikar Rising #120
 * Sorcery // Land · Modal double-faced card
 *
 * Front — Pelakka Predation: "Target opponent reveals their hand. You choose a card from it with
 * mana value 3 or greater. That player discards that card."
 * Back — Pelakka Caverns: "This land enters tapped. {T}: Add {B}."
 *
 * Duress-shaped pipeline with a mana-value filter. A modal double-faced card in hand has its
 * front face's mana value, and {X} counts as 0 there.
 */
private val PelakkaPredationFront = card("Pelakka Predation") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose a card from it with mana value 3 or greater. " +
        "That player discards that card."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            run(Effects.RevealHand(opponent))
            val opponentHand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
            val toDiscard = chooseExactly(
                1,
                from = opponentHand,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Any.manaValueAtLeast(3),
                prompt = "Choose a card with mana value 3 or greater to discard",
                alwaysPrompt = true,
                showAllCards = true
            )
            discard(toDiscard, opponent.asPlayer)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "120"
        artist = "Jokubas Uogintas"
        flavorText = "\"I knew we shouldn't have come this way.\"\n—Sneeflebrix, expedition rogue"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e63f8b20-f45b-4293-9aac-cdc021939be6.jpg?1783929374"
        ruling("2020-09-25", "If a card in a player's hand has {X} in its mana cost, X is considered to be 0.")
    }
}

private val PelakkaCavernsBack = card("Pelakka Caverns") {
    typeLine = "Land"
    colorIdentity = "B"
    oracleText = "This land enters tapped.\n{T}: Add {B}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "120"
        artist = "Jokubas Uogintas"
        flavorText = "\"As a child, I was afraid of the dark. Now, I'm afraid of the things I know are lurking in the dark, waiting to bite my head off.\"\n—Sneeflebrix, expedition rogue"
        imageUri = "https://cards.scryfall.io/normal/back/e/6/e63f8b20-f45b-4293-9aac-cdc021939be6.jpg?1783929374"
    }
}

val PelakkaPredation: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = PelakkaPredationFront,
    backFace = PelakkaCavernsBack,
)
