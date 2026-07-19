package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Feed the Cauldron
 * {2}{B}
 * Instant
 * Destroy target creature with mana value 3 or less. If it's your turn, create a Food token.
 *
 * The "if it's your turn" clause is checked on resolution (it is not an intervening-if — the
 * spell has no trigger condition), so [Conditions.IsYourTurn] gates only the Food half. The
 * destroy still happens on an opponent's turn; if the only target is illegal on resolution the
 * whole spell is countered and no Food is created.
 */
val FeedTheCauldron = card("Feed the Cauldron") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target creature with mana value 3 or less. If it's your turn, create a Food token. " +
        "(It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")"

    spell {
        val t = target("target", TargetCreature(filter = TargetFilter.Creature.manaValueAtMost(3)))
        effect = Effects.Move(t, Zone.GRAVEYARD, byDestruction = true) then
            ConditionalEffect(
                condition = Conditions.IsYourTurn,
                effect = Effects.CreateFood()
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "91"
        artist = "Marta Nael"
        flavorText = "Kellan heard Agatha howl when he slammed into her, heard her scream as she fell into the cauldron, " +
            "but couldn't afford to think about the implications just yet."
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0cfd18a5-e06a-4cb5-b78e-de18ec641321.jpg?1783915107"
    }
}
