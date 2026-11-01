package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rampage of the Clans — Ravnica Allegiance #134
 * {3}{G} · Instant
 *
 * Destroy all artifacts and enchantments. For each permanent destroyed this way, its controller
 * creates a 3/3 green Centaur creature token.
 *
 * The Phasing of Zhalfir's shape: [Effects.DestroyAll] with `storeDestroyedAs` records only the
 * permanents actually destroyed (indestructible ones stay out), and each iteration mints the token
 * for that permanent's last-known controller via [Player.ControllerOfIterationEntity]. All the
 * destruction happens before any Centaur is created.
 */
val RampageOfTheClans = card("Rampage of the Clans") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Destroy all artifacts and enchantments. For each permanent destroyed this way, its " +
        "controller creates a 3/3 green Centaur creature token."

    spell {
        effect = Effects.Pipeline {
            val destroyed = runStoringCollection {
                Effects.DestroyAll(GameObjectFilter.ArtifactOrEnchantment, storeDestroyedAs = it)
            }
            run(
                Effects.ForEachInCollection(
                    destroyed,
                    Effects.CreateToken(
                        power = 3,
                        toughness = 3,
                        colors = setOf(Color.GREEN),
                        creatureTypes = setOf("Centaur"),
                        controller = EffectTarget.PlayerRef(Player.ControllerOfIterationEntity),
                        imageUri = "https://cards.scryfall.io/normal/front/5/f/5fc993a7-a1ce-4403-a0a0-2afc9f9eca42.jpg?1783933603",
                    )
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "134"
        artist = "Sidharth Chaturvedi"
        flavorText = "\"Rage! Rage until the whole world burns!\""
        imageUri = "https://cards.scryfall.io/normal/front/2/8/28ebab7d-d03d-4473-aa9b-485aebb66433.jpg?1786206963"
        ruling(
            "2019-01-25",
            "All of the artifacts and enchantments that are destroyed leave the battlefield before any " +
                "Centaurs are created, and their abilities can't affect or trigger on these tokens entering " +
                "the battlefield."
        )
        ruling(
            "2019-01-25",
            "If a permanent has indestructible, it isn't destroyed this way and its controller won't create " +
                "a Centaur token for it. If it is destroyed but put into a zone other than a graveyard, its " +
                "controller does create a Centaur token for it."
        )
    }
}
