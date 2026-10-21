package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Campus Renovation
 * {3}{R}{W}
 * Sorcery
 *
 * Return up to one target artifact or enchantment card from your graveyard to the battlefield.
 * Exile the top two cards of your library. Until the end of your next turn, you may play those cards.
 *
 * The reanimation target is optional (Repair and Recharge's filter, "up to one"); the impulse half is
 * Commune with Lava's recipe with a fixed count of two. Per the ruling, a chosen target that has become
 * illegal makes the whole spell fizzle, so nothing is exiled.
 */
val CampusRenovation = card("Campus Renovation") {
    manaCost = "{3}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Sorcery"
    oracleText = "Return up to one target artifact or enchantment card from your graveyard to the battlefield. " +
        "Exile the top two cards of your library. Until the end of your next turn, you may play those cards."

    spell {
        val returned = target(
            TargetFilter(GameObjectFilter.ArtifactOrEnchantment.ownedByYou(), zone = Zone.GRAVEYARD),
            optional = true,
        )
        effect = Effects.Move(returned, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD) then
            Patterns.Exile.impulse(
                count = 2,
                expiry = MayPlayExpiry.UntilControllerStep(Step.CLEANUP, includeCurrentTurn = false)
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "27"
        artist = "Robin Olausson"
        flavorText = "Reconstructing the past is Lorehold's specialty."
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6d925d13-fcd6-417b-b2b2-bbdd114aae78.jpg?1783916518"
    }
}
