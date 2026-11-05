package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Verdant Rebirth
 * {1}{G}
 * Instant
 * Until end of turn, target creature gains "When this creature dies, return it to its owner's hand."
 * Draw a card.
 *
 * Same granted self-dies trigger as Ashnod's Intervention; the return only moves the card from the
 * graveyard (`fromZone`), so if it has left the graveyard before the trigger resolves it stays put.
 */
val VerdantRebirth = card("Verdant Rebirth") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Until end of turn, target creature gains \"When this creature dies, return it to its " +
        "owner's hand.\"\nDraw a card."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.self.dies(),
                effect = Effects.Move(EffectTarget.Self, Zone.HAND, fromZone = Zone.GRAVEYARD),
                descriptionOverride = "When this creature dies, return it to its owner's hand.",
            ),
            target = t,
            duration = Duration.EndOfTurn,
        ) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "212"
        artist = "Josu Hernaiz"
        flavorText = "The dryads of the forest have no stake in Ixalan's conflict, but they hate to see " +
            "any living being suffer."
        imageUri = "https://cards.scryfall.io/normal/front/9/9/993f4343-8f0a-4c50-b6fd-49f1f11d96f0.jpg?1783935718"
    }
}
