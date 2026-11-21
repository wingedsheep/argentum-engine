package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedByFewerThan
import com.wingedsheep.sdk.scripting.CardsInZonesCantBeTargeted
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Underworld Cerberus
 * {3}{B}{R}
 * Creature — Dog
 * 6/6
 *
 * This creature can't be blocked except by three or more creatures.
 * Cards in graveyards can't be the targets of spells or abilities.
 * When this creature dies, exile it and each player returns all creature cards from their
 * graveyard to their hand.
 *
 * - Blocking: the generalized-menace static [CantBeBlockedByFewerThan] with three blockers.
 * - Graveyard lock: [CardsInZonesCantBeTargeted]'s defaults (graveyards, any card, any
 *   controller) — Ground Seal / Silent Gravestone's static.
 * - Dies trigger: exile Cerberus from the graveyard first, so it isn't swept back to hand, then
 *   Empty the Catacombs' symmetric gather-and-return (cards moved to hand go to their owner's
 *   hand). The exile is keyed on `fromZone = GRAVEYARD`: if Cerberus already left the graveyard,
 *   the exile does nothing and the return still happens (2013-09-15 ruling).
 */
val UnderworldCerberus = card("Underworld Cerberus") {
    manaCost = "{3}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Dog"
    power = 6
    toughness = 6
    oracleText = "This creature can't be blocked except by three or more creatures.\n" +
        "Cards in graveyards can't be the targets of spells or abilities.\n" +
        "When this creature dies, exile it and each player returns all creature cards from their graveyard to their hand."

    staticAbility {
        ability = CantBeBlockedByFewerThan(3)
    }

    staticAbility {
        ability = CardsInZonesCantBeTargeted()
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.Pipeline {
            run(Effects.Move(EffectTarget.Self, Zone.EXILE, fromZone = Zone.GRAVEYARD))
            val graveyardCreatures = gather(
                CardSource.FromZone(
                    zone = Zone.GRAVEYARD,
                    player = Player.Each,
                    filter = GameObjectFilter.Creature,
                )
            )
            toHand(graveyardCreatures)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "208"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a078c3d-88db-4459-9667-ce610ae2f660.jpg?1783939723"

        ruling("2013-09-15", "If you can't exile Underworld Cerberus when its last ability resolves, perhaps because it's been exiled by another spell or ability, each player will still return all creature cards from their graveyard to their hand.")
        ruling("2013-09-15", "If two of these die at the same time, the last ability of each will trigger. If each player controlled one, the nonactive player's ability will resolve first. The Underworld Cerberus controlled by that player will be exiled, and all other creature cards in graveyards (including the Underworld Cerberus controlled by the active player) will be returned to their owners' hands. Then the active player's ability will resolve. That Underworld Cerberus won't be exiled, but any creature cards in graveyards at that time, perhaps because they died in response to this ability, will be returned to their owners' hands.")
    }
}
