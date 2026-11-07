package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Saurian Symbiote — Foundations Jumpstart #23
 * {3}{G} · Creature — Fungus Dinosaur · 2/3
 *
 * Reach
 * When this creature enters, choose one —
 * • Put a +1/+1 counter on this creature.
 * • Create a 1/1 green Saproling creature token.
 */
val SaurianSymbiote = card("Saurian Symbiote") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Fungus Dinosaur"
    power = 2
    toughness = 3
    oracleText = "Reach (This creature can block creatures with flying.)\n" +
        "When this creature enters, choose one —\n" +
        "• Put a +1/+1 counter on this creature.\n" +
        "• Create a 1/1 green Saproling creature token."

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
                "Put a +1/+1 counter on this creature"
            ),
            Mode.noTarget(
                Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.GREEN),
                    creatureTypes = setOf("Saproling")
                ),
                "Create a 1/1 green Saproling creature token"
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "23"
        artist = "Aldo Domínguez"
        flavorText = "It's a living, self-contained ecosystem."
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b32224ef-cfd4-42ef-857a-24386590c360.jpg?1783908864"
    }
}
