package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Silent Skimmer
 * {3}{B}
 * Creature — Eldrazi Drone
 * 0/4
 * Devoid (This card has no color.)
 * Flying
 * Whenever this creature attacks, defending player loses 2 life.
 */
val SilentSkimmer = card("Silent Skimmer") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Eldrazi Drone"
    power = 0
    toughness = 4
    oracleText = "Devoid (This card has no color.)\n" +
        "Flying\n" +
        "Whenever this creature attacks, defending player loses 2 life."

    keywords(Keyword.DEVOID, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.DefendingPlayer))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "96"
        artist = "Alejandro Mirabal"
        flavorText = "\"I watched it pass at a distance. When I looked back to my companions, Najet was dead.\"\n" +
            "—Akiri, kor line-slinger"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9b715fc0-9c36-4619-8d65-6d1064ed8a32.jpg?1783938204"
    }
}
