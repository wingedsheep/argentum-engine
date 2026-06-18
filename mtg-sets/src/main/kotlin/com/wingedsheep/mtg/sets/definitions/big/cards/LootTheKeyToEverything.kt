package com.wingedsheep.mtg.sets.definitions.big.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Loot, the Key to Everything
 * {G}{U}{R}
 * Legendary Creature — Beast Noble
 * 1/2
 *
 * Ward {1}
 * At the beginning of your upkeep, exile the top X cards of your library, where X is the
 * number of card types among other nonland permanents you control. You may play those
 * cards this turn.
 *
 * Modeling: X is [DynamicAmount.AggregateBattlefield] with [Aggregation.DISTINCT_TYPES] over
 * your nonland permanents, `excludeSelf = true` ("among *other* nonland permanents"). The
 * impulse exile reuses the Outpost Siege / Light Up the Stage shape — Gather top X →
 * Move-to-exile → [GrantMayPlayFromExileEffect] with [MayPlayExpiry.EndOfTurn].
 */
val LootTheKeyToEverything = card("Loot, the Key to Everything") {
    manaCost = "{G}{U}{R}"
    colorIdentity = "GUR"
    typeLine = "Legendary Creature — Beast Noble"
    power = 1
    toughness = 2
    oracleText = "Ward {1}\n" +
        "At the beginning of your upkeep, exile the top X cards of your library, where X is " +
        "the number of card types among other nonland permanents you control. You may play " +
        "those cards this turn."

    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{1}")))

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = Effects.Composite(listOf(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(
                    DynamicAmount.AggregateBattlefield(
                        player = Player.You,
                        filter = GameObjectFilter.NonlandPermanent,
                        aggregation = Aggregation.DISTINCT_TYPES,
                        excludeSelf = true
                    )
                ),
                storeAs = "exiledCards"
            ),
            MoveCollectionEffect(
                from = "exiledCards",
                destination = CardDestination.ToZone(Zone.EXILE)
            ),
            GrantMayPlayFromExileEffect("exiledCards", MayPlayExpiry.EndOfTurn)
        ))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "21"
        artist = "Rudy Siswanto"
        flavorText = "The vault's ultimate prize wasn't exactly what Kellan had imagined."
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb169fa2-c92e-45f7-89a2-0ca0e3910a1c.jpg?1739804220"
    }
}
