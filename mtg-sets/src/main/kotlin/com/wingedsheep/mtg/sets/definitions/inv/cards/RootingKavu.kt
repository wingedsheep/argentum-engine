package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rooting Kavu
 * {2}{G}{G}
 * Creature — Kavu
 * 4/3
 * When this creature dies, you may exile it. If you do, shuffle all creature cards
 * from your graveyard into your library.
 *
 * The dies trigger functions from the graveyard (`triggerZone = Zone.GRAVEYARD`) so
 * "exile it" can reference Self. Exiling the card always succeeds once the player
 * chooses to, so the "if you do" shuffle is composed under the same MayEffect.
 */
val RootingKavu = card("Rooting Kavu") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Kavu"
    power = 4
    toughness = 3
    oracleText = "When this creature dies, you may exile it. If you do, shuffle all " +
        "creature cards from your graveyard into your library."

    triggeredAbility {
        trigger = Triggers.Dies
        triggerZone = Zone.GRAVEYARD
        effect = MayEffect(
            effect = CompositeEffect(
                listOf(
                    Effects.Exile(EffectTarget.Self),
                    GatherCardsEffect(
                        source = CardSource.FromZone(
                            Zone.GRAVEYARD,
                            Player.You,
                            GameObjectFilter.Creature
                        ),
                        storeAs = "graveyardCreatures"
                    ),
                    MoveCollectionEffect(
                        from = "graveyardCreatures",
                        destination = CardDestination.ToZone(
                            Zone.LIBRARY,
                            Player.You,
                            ZonePlacement.Shuffled
                        )
                    )
                )
            ),
            descriptionOverride = "You may exile this creature. If you do, shuffle all " +
                "creature cards from your graveyard into your library."
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "207"
        artist = "Heather Hudson"
        imageUri = "https://cards.scryfall.io/normal/front/1/2/12c25a4c-d93a-402b-999f-0b9919123cc5.jpg?1562898755"
    }
}
