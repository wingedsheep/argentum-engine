package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Soul Nova — Mirrodin #25
 * {3}{W}{W} · Instant
 *
 * Exile target attacking creature and all Equipment attached to it.
 *
 * The Equipment has to be **gathered before the creature leaves**: a permanent's attachment list
 * is cleared the moment it changes zones, so a [CardSource.AttachedTo] read after the exile would
 * come back empty. Hence the pipeline order — gather the attached Equipment into a slot, exile the
 * creature, then exile the collection. (Auras and Fortifications are deliberately not swept up;
 * the card names Equipment only, and an Aura left attached to nothing dies to the CR 704.5m
 * state-based action anyway.)
 */
val SoulNova = card("Soul Nova") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Exile target attacking creature and all Equipment attached to it."

    spell {
        val creature = target("target attacking creature", Targets.AttackingCreature)
        effect = Effects.Pipeline(
            descriptionOverride = "Exile target attacking creature and all Equipment attached to it."
        ) {
            val equipment = gather(
                CardSource.AttachedTo(
                    host = creature,
                    filter = GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT)
                )
            )
            run(Effects.Exile(creature))
            exile(equipment)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "25"
        artist = "Keith Garletts"
        flavorText = "Within seconds, the nim was consumed in blinding sunfire. Afterwards, only a " +
            "puddle of molten iron remained."
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f862682d-cbf3-4c35-83d0-76883b0ac105.jpg?1783944557"
    }
}
