package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Possibility Technician
 * {2}{R}
 * Creature — Kavu Artificer
 * 3/3
 * Whenever this creature or another Kavu you control enters, exile the top card of your library.
 *   For as long as that card remains exiled, you may play it if you control a Kavu.
 * Warp {1}{R}
 */
val PossibilityTechnician = card("Possibility Technician") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Kavu Artificer"
    power = 3
    toughness = 3
    oracleText = "Whenever this creature or another Kavu you control enters, exile the top card of your library. For as long as that card remains exiled, you may play it if you control a Kavu.\n" +
        "Warp {1}{R} (You may cast this card from your hand for its warp cost. Exile this creature at the beginning of the next end step, then you may cast it from exile on a later turn.)"

    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Creature.withSubtype(Subtype.KAVU).youControl(),
                to = Zone.BATTLEFIELD
            ),
            binding = TriggerBinding.ANY
        )
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
                    storeAs = "exiledCard"
                ),
                MoveCollectionEffect(
                    from = "exiledCard",
                    destination = CardDestination.ToZone(Zone.EXILE)
                ),
                Effects.GrantMayPlayFromExile(
                    from = "exiledCard",
                    expiry = MayPlayExpiry.Permanent,
                    condition = Exists(
                        player = Player.You,
                        zone = Zone.BATTLEFIELD,
                        filter = GameObjectFilter.Creature.withSubtype(Subtype.KAVU)
                    )
                )
            )
        )
    }

    warp = "{1}{R}"

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "153"
        artist = "Antonio José Manzanedo"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b146c78-403f-48c8-941d-41114498bb89.jpg?1752947171"
    }
}
