package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Depopulate — Streets of New Capenna #10
 * {2}{W}{W} · Sorcery
 *
 * Each player who controls a multicolored creature draws a card. Then destroy all creatures.
 *
 * The Fade from History shape: one [Effects.ForEachPlayer] over [Player.Each] whose body is an
 * [Effects.If]. Inside the loop the controller is rebound to the iterated player, so
 * [Conditions.YouControl] asks about *that* player and [Effects.DrawCards] draws for them.
 * Drawing a card can't change which creatures anyone controls, so sequential iteration reads the
 * same battlefield for every player; the wipe comes after every draw.
 */
val Depopulate = card("Depopulate") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Each player who controls a multicolored creature draws a card. Then destroy all creatures."

    spell {
        effect = Effects.ForEachPlayer(
            players = Player.Each,
            effect = Effects.If(
                condition = Conditions.YouControl(GameObjectFilter.Creature and GameObjectFilter.Multicolored),
                then = Effects.DrawCards(1),
            ),
        ) then
            Effects.DestroyAll(GameObjectFilter.Creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "10"
        artist = "Jokubas Uogintas"
        flavorText = "No screams rang out. No blood stained the streets. In an instant, the bustling " +
            "metropolis simply fell silent."
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c53c1898-9107-4bf8-b249-d0502fb9596d.jpg?1783923159"
    }
}
