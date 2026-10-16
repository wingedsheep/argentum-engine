package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Super Mutant Scavenger
 * {4}{G}
 * Creature — Mutant Warrior
 * 5/5
 * Trample
 * When this creature enters or dies, return up to one target Aura or Equipment card from your
 * graveyard to your hand.
 *
 * "Enters or dies" is two separate triggered abilities sharing one printed sentence; each
 * chooses its own (optional) target.
 */
val SuperMutantScavenger = card("Super Mutant Scavenger") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Mutant Warrior"
    power = 5
    toughness = 5
    oracleText = "Trample\n" +
        "When this creature enters or dies, return up to one target Aura or Equipment card from your " +
        "graveyard to your hand."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val card = target(
            TargetFilter(
                GameObjectFilter.Any.withAnyOfSubtypes(listOf(Subtype.AURA, Subtype.EQUIPMENT)).ownedByYou(),
                zone = Zone.GRAVEYARD
            ),
            optional = true
        )
        effect = Effects.Move(card, Zone.HAND)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        val card = target(
            TargetFilter(
                GameObjectFilter.Any.withAnyOfSubtypes(listOf(Subtype.AURA, Subtype.EQUIPMENT)).ownedByYou(),
                zone = Zone.GRAVEYARD
            ),
            optional = true
        )
        effect = Effects.Move(card, Zone.HAND)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "85"
        artist = "David Astruga"
        flavorText = "Out in the wastelands, everything's a steal."
        imageUri = "https://cards.scryfall.io/normal/front/4/1/41f48a99-eaf7-42ec-bab3-9ebbc735a82a.jpg?1783912394"
    }
}
