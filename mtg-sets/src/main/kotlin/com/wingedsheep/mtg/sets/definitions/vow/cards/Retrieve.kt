package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Retrieve
 * {2}{G}
 * Sorcery
 *
 * Return up to one target creature card and up to one target noncreature permanent card from your
 * graveyard to your hand. Exile Retrieve.
 *
 * Two independent "up to one target" graveyard slots (optional, minCount 0) — the first a creature
 * card, the second a noncreature permanent card you own in your graveyard — each returned to hand.
 * `selfExile()` routes the spell to exile instead of its owner's graveyard as it finishes resolving
 * (Wisdom of Ages idiom).
 */
val Retrieve = card("Retrieve") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Return up to one target creature card and up to one target noncreature permanent " +
        "card from your graveyard to your hand. Exile Retrieve."

    spell {
        selfExile()
        val creature = target(
            "creature",
            TargetObject(optional = true, filter = TargetFilter.CreatureInYourGraveyard)
        )
        val noncreature = target(
            "noncreature permanent",
            TargetObject(
                optional = true,
                filter = TargetFilter(
                    GameObjectFilter.NoncreaturePermanent.ownedByYou(),
                    zone = Zone.GRAVEYARD
                )
            )
        )
        effect = Effects.Composite(
            listOf(
                Effects.ReturnToHand(creature),
                Effects.ReturnToHand(noncreature)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "215"
        artist = "Alix Branwyn"
        flavorText = "The roots preserved the armor for a hundred years, safeguarding it for a " +
            "traveler in need."
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e997f78-22a2-4b66-ac10-1adc9a72ce3b.jpg?1782703042"
    }
}
