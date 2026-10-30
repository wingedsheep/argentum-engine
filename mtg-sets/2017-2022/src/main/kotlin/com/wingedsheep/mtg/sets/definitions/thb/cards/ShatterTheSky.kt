package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Shatter the Sky
 * {2}{W}{W}
 * Sorcery
 * Each player who controls a creature with power 4 or greater draws a card. Then destroy all creatures.
 *
 * Depopulate's shape with a power-4-or-greater check: one [Effects.ForEachPlayer] over
 * [Player.Each] whose body is an [Effects.If]; inside the loop the controller is the iterated
 * player, so [Conditions.YouControl] asks about them. Drawing can't change anyone's board, so every
 * player is checked against the same battlefield, and the wipe follows every draw.
 */
val ShatterTheSky = card("Shatter the Sky") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Each player who controls a creature with power 4 or greater draws a card. " +
        "Then destroy all creatures."

    spell {
        effect = Effects.ForEachPlayer(
            players = Player.Each,
            effect = Effects.If(
                condition = Conditions.YouControl(GameObjectFilter.Creature.powerAtLeast(4)),
                then = Effects.DrawCards(1),
            ),
        ) then
            Effects.DestroyAll(GameObjectFilter.Creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "37"
        artist = "Bayard Wu"
        flavorText = "When the gods go to war, mortals pay the highest price."
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b706977b-db8e-4810-882d-ed3745404489.jpg?1783931589"
        ruling("2020-01-24", "Shatter the Sky destroys all creatures, even if no player draws a card.")
    }
}
