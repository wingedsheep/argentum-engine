package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hubris — Journey into Nyx #41 (canonical printing)
 * {1}{U} · Instant
 *
 * Return target creature and all Auras attached to it to their owners' hands.
 *
 * The Mark of Eviction shape: the Auras attached to the target are gathered while still attached,
 * then the creature and those Auras return in the same resolution, so no Aura is left unattached
 * long enough for state-based actions to put it into a graveyard. A bestowed Aura is gathered as
 * an Aura and returns with its host. Equipment is left on the battlefield.
 */
val Hubris = card("Hubris") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Return target creature and all Auras attached to it to their owners' hands."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Pipeline {
            val auras = gather(
                CardSource.AttachedTo(
                    host = creature,
                    filter = GameObjectFilter.Permanent.withSubtype(Subtype.AURA),
                )
            )
            run(Effects.ReturnToHand(creature))
            toHand(auras)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "41"
        artist = "Seb McKinnon"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/05f5b473-edad-43a9-b4e7-b6d3fc877cf2.jpg?1783939447"
    }
}
