package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.OptionalCostEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Reno and Rude
 * {1}{B}
 * Legendary Creature — Human Assassin
 * 2/1
 *
 * Menace
 * Whenever Reno and Rude deals combat damage to a player, exile the top card of
 * that player's library. Then you may sacrifice another creature or artifact. If
 * you do, you may play the exiled card this turn, and mana of any type can be
 * spent to cast it.
 *
 * Resolution-time chain over existing pipeline primitives:
 *   1. exile the top card of the *damaged* player's library ([Player.TriggeringPlayer]
 *      is the player dealt combat damage; the card stays owned by them in exile),
 *   2. an [OptionalCostEffect] models "you may sacrifice another creature or artifact.
 *      If you do, ...": the sacrifice is the optional cost (`excludeSource` makes it
 *      "another"), and only paying it grants the play permission,
 *   3. the reward is a [GrantMayPlayFromExileEffect] with `withAnyManaType` for
 *      "mana of any type can be spent to cast it", expiring at end of turn.
 */
val RenoAndRude = card("Reno and Rude") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Assassin"
    power = 2
    toughness = 1
    oracleText = "Menace\n" +
        "Whenever Reno and Rude deals combat damage to a player, exile the top card of " +
        "that player's library. Then you may sacrifice another creature or artifact. If you " +
        "do, you may play the exiled card this turn, and mana of any type can be spent to cast it."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(
                        DynamicAmount.Fixed(1),
                        player = Player.TriggeringPlayer,
                    ),
                    storeAs = "exiled",
                ),
                MoveCollectionEffect(
                    from = "exiled",
                    destination = CardDestination.ToZone(Zone.EXILE, player = Player.TriggeringPlayer),
                ),
                OptionalCostEffect(
                    cost = SacrificeEffect(
                        filter = GameObjectFilter.CreatureOrArtifact,
                        count = 1,
                        excludeSource = true,
                    ),
                    ifPaid = GrantMayPlayFromExileEffect(
                        from = "exiled",
                        expiry = MayPlayExpiry.EndOfTurn,
                        withAnyManaType = true,
                    ),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "113"
        artist = "Maji"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5eb0064-c7c4-4e3e-add2-b86269de3fb9.jpg?1748706185"
    }
}
