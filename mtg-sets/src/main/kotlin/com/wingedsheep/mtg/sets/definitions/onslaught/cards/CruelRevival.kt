package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeRegeneratedEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.targeting.TargetCreature
import com.wingedsheep.sdk.targeting.TargetObject

/**
 * Cruel Revival
 * {4}{B}
 * Instant
 * Destroy target non-Zombie creature. It can't be regenerated.
 * Return up to one target Zombie card from your graveyard to your hand.
 */
val CruelRevival = card("Cruel Revival") {
    manaCost = "{4}{B}"
    typeLine = "Instant"
    oracleText = "Destroy target non-Zombie creature. It can't be regenerated.\nReturn up to one target Zombie card from your graveyard to your hand."

    spell {
        val creature = target(
            "non-Zombie creature",
            TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.notSubtype(Subtype("Zombie"))))
        )
        val zombieCard = target(
            "Zombie card in your graveyard",
            TargetObject(
                optional = true,
                filter = TargetFilter(
                    GameObjectFilter.Any.withSubtype("Zombie").ownedByYou(),
                    zone = Zone.GRAVEYARD
                )
            )
        )

        effect = CantBeRegeneratedEffect(creature) then
                MoveToZoneEffect(creature, Zone.GRAVEYARD, byDestruction = true) then
                MoveToZoneEffect(zombieCard, Zone.HAND)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "135"
        artist = "Greg Staples"
        flavorText = "Few Cabal fighters fear death. They fear what follows it."
        imageUri = "https://cards.scryfall.io/normal/front/2/4/245aba23-2abb-4084-b4cb-d06e46de2108.jpg?1562903595"
    }
}