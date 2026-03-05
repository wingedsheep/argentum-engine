package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Siege Rhino
 * {1}{W}{B}{G}
 * Creature — Rhino
 * 4/5
 * Trample
 * When Siege Rhino enters the battlefield, each opponent loses 3 life and you gain 3 life.
 */
val SiegeRhino = card("Siege Rhino") {
    manaCost = "{1}{W}{B}{G}"
    typeLine = "Creature — Rhino"
    power = 4
    toughness = 5
    oracleText = "Trample\nWhen this creature enters, each opponent loses 3 life and you gain 3 life."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.LoseLife(3, EffectTarget.PlayerRef(Player.EachOpponent))
            .then(Effects.GainLife(3))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "200"
        artist = "Volkan Baǵa"
        flavorText = "The mere approach of an Abzan war beast is enough to send enemies fleeing in panic."
        imageUri = "https://cards.scryfall.io/normal/front/9/0/9011126a-20bd-4c86-a63b-1691f79ac247.jpg?1562790317"
    }
}
