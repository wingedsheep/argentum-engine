package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cryptbreaker — Eldritch Moon #86
 * {B} · Creature — Zombie · 1/1
 *
 * {1}{B}, {T}, Discard a card: Create a 2/2 black Zombie creature token.
 * Tap three untapped Zombies you control: You draw a card and lose 1 life.
 *
 * The second cost taps any three untapped Zombie permanents you control — Cryptbreaker itself
 * included, and summoning-sick ones too, since it isn't the {T} symbol (see ruling).
 */
val Cryptbreaker = card("Cryptbreaker") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 1
    toughness = 1
    oracleText = "{1}{B}, {T}, Discard a card: Create a 2/2 black Zombie creature token.\n" +
        "Tap three untapped Zombies you control: You draw a card and lose 1 life."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B}"), Costs.Tap, Costs.DiscardCard)
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            imageUri = "https://cards.scryfall.io/normal/front/1/5/15f1f43a-cbdb-4d1b-a254-dadcb2469784.jpg?1783937418"
        )
    }

    activatedAbility {
        cost = Costs.TapPermanents(3, GameObjectFilter.Permanent.withSubtype("Zombie"))
        effect = Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "86"
        artist = "Darek Zabrocki"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/629e37d1-c0f3-44f2-926e-41eb3687c1d9.jpg?1783937487"
        ruling(
            "2016-07-13",
            "You can tap any three untapped Zombies you control, including ones you haven't controlled continuously since the beginning of your most recent turn, to pay the cost of Cryptbreaker's second activated ability. This includes Cryptbreaker itself."
        )
    }
}
