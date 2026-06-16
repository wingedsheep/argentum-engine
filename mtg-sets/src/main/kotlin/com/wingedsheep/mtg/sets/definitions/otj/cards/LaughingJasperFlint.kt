package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantAdditionalTypesToGroup
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Laughing Jasper Flint
 * {1}{B}{R}
 * Legendary Creature — Lizard Rogue
 * 4/3
 *
 * Creatures you control but don't own are Mercenaries in addition to their other types.
 * At the beginning of your upkeep, exile the top X cards of target opponent's library, where X is
 * the number of outlaws you control. Until end of turn, you may cast spells from among those cards,
 * and mana of any type can be spent to cast those spells.
 *
 * The type-grant uses [GrantAdditionalTypesToGroup] over a "you control but an opponent owns"
 * filter (`ControlledByYou` AND `OwnedByOpponent`). The upkeep theft gathers the top X (X =
 * outlaws you control, [Filters.OutlawCreature]) from the targeted opponent's library, moves them
 * to exile, and grants a may-play-from-exile permission with `withAnyManaType = true` expiring at
 * end of turn — the same primitive Cruelclaw's Heist uses for "mana of any type can be spent".
 */
val LaughingJasperFlint = card("Laughing Jasper Flint") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Lizard Rogue"
    power = 4
    toughness = 3
    oracleText = "Creatures you control but don't own are Mercenaries in addition to their other " +
        "types.\n" +
        "At the beginning of your upkeep, exile the top X cards of target opponent's library, " +
        "where X is the number of outlaws you control. Until end of turn, you may cast spells from " +
        "among those cards, and mana of any type can be spent to cast those spells."

    staticAbility {
        ability = GrantAdditionalTypesToGroup(
            filter = GroupFilter(
                GameObjectFilter.Creature.withControllerPredicate(
                    ControllerPredicate.And(
                        listOf(
                            ControllerPredicate.ControlledByYou,
                            ControllerPredicate.OwnedByOpponent,
                        )
                    )
                )
            ),
            addSubtypes = listOf("Mercenary"),
        )
    }

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        val opponent = target("target opponent", Targets.Opponent)
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(
                        count = DynamicAmount.AggregateBattlefield(
                            player = Player.You,
                            filter = Filters.OutlawCreature,
                        ),
                        player = Player.ContextPlayer(0),
                    ),
                    storeAs = "stolenCards",
                ),
                MoveCollectionEffect(
                    from = "stolenCards",
                    destination = CardDestination.ToZone(Zone.EXILE, Player.ContextPlayer(0)),
                ),
                GrantMayPlayFromExileEffect(
                    from = "stolenCards",
                    expiry = MayPlayExpiry.EndOfTurn,
                    withAnyManaType = true,
                ),
            )
        )
        description = "At the beginning of your upkeep, exile the top X cards of target opponent's " +
            "library, where X is the number of outlaws you control. Until end of turn, you may cast " +
            "spells from among those cards, and mana of any type can be spent to cast those spells."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "215"
        artist = "Francis Tneh"
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af0b3a41-ba99-41e8-bcfb-5796500c17c7.jpg?1712356142"

        ruling("2024-04-12", "X is determined as the ability resolves, counting the outlaws you " +
            "control at that time. (Assassins, Mercenaries, Pirates, Rogues, and Warlocks are outlaws.)")
        ruling("2024-04-12", "You must still follow the normal timing permissions and restrictions " +
            "of the cards you cast this way. Casting a sorcery, for example, is only possible during " +
            "your main phase while the stack is empty.")
        ruling("2024-04-12", "Mana of any type can be spent to cast the exiled cards, but you still " +
            "must pay those costs. A card with {X} in its cost can have X paid with mana of any type.")
        ruling("2024-04-12", "Any cards you don't cast remain in exile.")
    }
}
