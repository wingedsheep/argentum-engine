package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Foulmire Knight // Profane Insight
 * {B}
 * Creature — Zombie Knight
 * 1/1
 * Deathtouch
 *
 * Adventure: Profane Insight — {2}{B}, Instant — Adventure
 * You draw a card and you lose 1 life.
 */
val FoulmireKnight = card("Foulmire Knight") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Knight"
    power = 1
    toughness = 1
    oracleText = "Deathtouch"

    keywords(Keyword.DEATHTOUCH)

    adventure("Profane Insight") {
        manaCost = "{2}{B}"
        typeLine = "Instant — Adventure"
        oracleText = "You draw a card and you lose 1 life. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            effect = Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "90"
        artist = "Alex Brock"
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c5f6c745-e46a-42eb-8eca-b7b74ab1245e.jpg?1783932638"
    }
}
