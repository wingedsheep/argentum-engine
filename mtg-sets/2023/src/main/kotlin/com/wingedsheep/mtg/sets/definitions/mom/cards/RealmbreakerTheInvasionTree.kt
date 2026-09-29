package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Realmbreaker, the Invasion Tree
 * {3}
 * Legendary Artifact
 * {2}, {T}: Target opponent mills three cards. Put a land card from their graveyard onto the
 * battlefield tapped under your control. It gains "If this land would leave the battlefield,
 * exile it instead of putting it anywhere else."
 * {10}, {T}, Sacrifice Realmbreaker: Search your library for any number of Praetor cards, put
 * them onto the battlefield, then shuffle.
 *
 * The land is gathered from the opponent's *whole* graveyard after the mill (the 2023-04-14
 * ruling), and the choice is mandatory ("Put a land card", not "you may"). The exile-on-leave
 * replacement is [Effects.GrantExileOnLeave] on the moved land, as on Kheru Lich Lord. The
 * Praetor search is unbounded, so it is an inline pipeline (Grozoth's shape) rather than
 * `Patterns.Library.searchLibrary`, which only offers "up to N".
 */
val RealmbreakerTheInvasionTree = card("Realmbreaker, the Invasion Tree") {
    manaCost = "{3}"
    typeLine = "Legendary Artifact"
    oracleText = "{2}, {T}: Target opponent mills three cards. Put a land card from their graveyard onto " +
        "the battlefield tapped under your control. It gains \"If this land would leave the battlefield, " +
        "exile it instead of putting it anywhere else.\"\n" +
        "{10}, {T}, Sacrifice Realmbreaker: Search your library for any number of Praetor cards, put them " +
        "onto the battlefield, then shuffle."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        target(Targets.Opponent)
        effect = Effects.Pipeline {
            mill(3, Player.TargetOpponent)
            val lands = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.TargetOpponent, GameObjectFilter.Land))
            val chosen = chooseExactly(
                1,
                from = lands,
                prompt = "Put a land card from their graveyard onto the battlefield under your control",
            )
            val land = moveTracked(
                chosen,
                CardDestination.ToZone(Zone.BATTLEFIELD, player = Player.You, placement = ZonePlacement.Tapped),
            ).asTarget
            run(Effects.GrantExileOnLeave(land))
        }
        description = "{2}, {T}: Target opponent mills three cards. Put a land card from their graveyard " +
            "onto the battlefield tapped under your control."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{10}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.Pipeline {
            val praetors = gather(
                CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Any.withSubtype(Subtype.PRAETOR))
            )
            val found = chooseAnyNumber(
                from = praetors,
                prompt = "Search your library for any number of Praetor cards",
            )
            move(found, CardDestination.ToZone(Zone.BATTLEFIELD, player = Player.You))
            run(Effects.ShuffleLibrary())
        }
        description = "{10}, {T}, Sacrifice Realmbreaker: Search your library for any number of Praetor " +
            "cards, put them onto the battlefield, then shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "263"
        artist = "Kekai Kotaki"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b608134-cfc3-48bf-92d8-35d732fcde54.jpg?1783916934"
        ruling("2023-04-14", "The land card you put onto the battlefield can be one that was just milled, or it can be one that was already in their graveyard.")
        ruling("2023-04-14", "In a multiplayer game, if a player leaves the game, all cards that player owns leave as well. If you leave the game, any lands you control from Realmbreaker's ability are exiled.")
    }
}
