package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CategoryPickPurpose

/**
 * Perilous Predicament — Aether Revolt #68
 * {4}{B} · Instant
 *
 * Each opponent sacrifices an artifact creature and a nonartifact creature of their choice.
 *
 * A per-category edict: each opponent, in APNAP order, picks one creature per category, then every
 * pick is sacrificed at the same time (2017-02-09 ruling) — so `chooseOnePerCategory` with the
 * SACRIFICE purpose feeding one `sacrifice`, not two edicts in a row. A category the opponent
 * controls nothing of is skipped, so a board of only artifact creatures loses one creature.
 */
val PerilousPredicament = card("Perilous Predicament") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Each opponent sacrifices an artifact creature and a nonartifact creature of their choice."

    spell {
        effect = Effects.Pipeline {
            val creatures = gather(GameObjectFilter.Creature.opponentControls())
            val doomed = chooseOnePerCategory(
                creatures,
                listOf(GameObjectFilter.ArtifactCreature, GameObjectFilter.Creature.nonartifact()),
                purpose = CategoryPickPurpose.SACRIFICE,
            )
            sacrifice(doomed)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "68"
        artist = "Ryan Alexander Lee"
        flavorText = "Comrades in life. Comrades in death."
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0edc708-0c76-4fcd-a175-d9c6f1ca3ac1.jpg?1783936759"
        ruling("2017-02-09", "When Perilous Predicament resolves, first the player whose turn it is (if that player is an opponent) chooses which creatures they will sacrifice, then each other opponent in turn order does the same, then all chosen creatures are sacrificed at the same time.")
        ruling("2017-02-09", "If each creature an opponent controls is an artifact creature, that player sacrifices only one creature. Similarly, if each creature an opponent controls is a nonartifact creature, that player sacrifices only one creature. If an opponent controls no creatures, that player doesn't sacrifice anything.")
    }
}
