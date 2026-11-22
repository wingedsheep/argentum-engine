package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Devout Invocation — Magic 2014 #16
 * {6}{W} · Sorcery
 *
 * Tap any number of untapped creatures you control. Create a 4/4 white Angel creature token with
 * flying for each creature tapped this way.
 *
 * Orphans of the Wheat's Gather → Select → Tap pipeline, chosen as the spell resolves (per the
 * ruling): gather your untapped creatures, choose any number of them on the battlefield (zero is
 * allowed), tap that selection, then create one Angel per creature in it.
 */
val DevoutInvocation = card("Devout Invocation") {
    manaCost = "{6}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Tap any number of untapped creatures you control. Create a 4/4 white Angel " +
        "creature token with flying for each creature tapped this way."

    spell {
        effect = Effects.Pipeline {
            val candidates = gather(
                CardSource.ControlledPermanents(
                    player = Player.You,
                    filter = GameObjectFilter.Creature.youControl().untapped()
                )
            )
            val tapped = chooseAnyNumber(
                from = candidates,
                prompt = "Tap any number of untapped creatures you control",
                useTargetingUI = true
            )
            run(Effects.TapCollection(tapped, tap = true))
            run(Effects.CreateToken(
                count = tapped.count,
                power = 4,
                toughness = 4,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Angel"),
                keywords = setOf(Keyword.FLYING),
                imageUri = "https://cards.scryfall.io/normal/front/8/2/826320b7-4ccf-4ace-8c2c-15052b6bad73.jpg?1783939887"
            ))
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "16"
        artist = "David Palumbo"
        flavorText = "A prayer to bring demons to their knees."
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8a286954-fb40-4440-9f0e-a28367c6823c.jpg?1783939944"
        ruling("2013-07-01", "You choose the creatures to tap as Devout Invocation resolves. Once it starts resolving, no player can respond to you tapping the creatures.")
    }
}
