package com.wingedsheep.mtg.sets.definitions.c14.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TriggeredAbility

/**
 * Reef Worm — Commander 2014 #16 (canonical printing; reprinted in A25, C21, MH3)
 * {3}{U} · Creature — Worm · 0/1
 *
 * When this creature dies, create a 3/3 blue Fish creature token with "When this token dies,
 * create a 6/6 blue Whale creature token with 'When this token dies, create a 9/9 blue Kraken
 * creature token.'"
 *
 * The chain is three nested inline tokens, each carrying its own dies trigger in
 * `triggeredAbilities` — the Kraken is vanilla.
 */
val ReefWorm = card("Reef Worm") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Worm"
    power = 0
    toughness = 1
    oracleText = "When this creature dies, create a 3/3 blue Fish creature token with \"When this " +
        "token dies, create a 6/6 blue Whale creature token with 'When this token dies, create a " +
        "9/9 blue Kraken creature token.'\""

    val kraken = Effects.CreateToken(
        power = 9,
        toughness = 9,
        colors = setOf(Color.BLUE),
        creatureTypes = setOf("Kraken"),
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b4ed9fe6-50cc-45e2-aeaa-df6e34eb7fe4.jpg?1783938798",
    )
    val whale = Effects.CreateToken(
        power = 6,
        toughness = 6,
        colors = setOf(Color.BLUE),
        creatureTypes = setOf("Whale"),
        triggeredAbilities = listOf(
            TriggeredAbility.create(trigger = Triggers.self.dies(), effect = kraken),
        ),
        imageUri = "https://cards.scryfall.io/normal/front/c/c/ccd172b9-377a-4dbf-a11a-e4985da62c72.jpg?1783938798",
    )
    val fish = Effects.CreateToken(
        power = 3,
        toughness = 3,
        colors = setOf(Color.BLUE),
        creatureTypes = setOf("Fish"),
        triggeredAbilities = listOf(
            TriggeredAbility.create(trigger = Triggers.self.dies(), effect = whale),
        ),
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f3cea7c-d092-410d-8c32-af0f4c8bc878.jpg?1783938798",
    )

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = fish
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "16"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b35e03a9-73e7-4577-a544-d39470678c76.jpg?1783938871"
    }
}
