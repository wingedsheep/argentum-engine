package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Crypt Incursion
 * {2}{B}
 * Instant
 * Exile all creature cards from target player's graveyard. You gain 3 life for each card exiled this way.
 *
 * The life gain counts the cards that actually reached exile (`moveTracked`), not the graveyard's
 * creature count before the move.
 */
val CryptIncursion = card("Crypt Incursion") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Exile all creature cards from target player's graveyard. You gain 3 life for each card exiled this way."

    spell {
        val player = target(Targets.Player)
        effect = Effects.Pipeline {
            val creatures = gather(
                CardSource.FromZone(Zone.GRAVEYARD, player.asPlayer, filter = GameObjectFilter.Creature)
            )
            val exiled = moveTracked(creatures, CardDestination.ToZone(Zone.EXILE, player.asPlayer))
            run(Effects.GainLife(DynamicAmounts.distinctEntitiesIn(exiled) * 3))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "23"
        artist = "Svetlin Velinov"
        flavorText = "The Golgari maze-runner Varolz hungers to prove his fortitude in battle, and his trail of victims feeds other hungers."
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3b71cc5-0a81-4cab-bae3-49335c04aaaa.jpg?1783940040"
    }
}
