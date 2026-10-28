package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.CantBlockUnless
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Relic Golem — Zendikar Rising #249
 * {3} · Artifact Creature — Golem · 6/6
 *
 * This creature can't attack or block unless an opponent has eight or more cards in their graveyard.
 * {2}, {T}: Target player mills two cards.
 *
 * Lupine Prototype's attack/block-restriction pair, gated on Mind Carver's "an opponent has eight or
 * more" test — the greatest graveyard among the controller's opponents, not a sum across them. The
 * restriction is checked only as attackers / blockers are declared, so a graveyard shrinking later
 * doesn't remove the Golem from combat (the printed rulings).
 */
private val OpponentHasEightInGraveyard = Conditions.CompareAmounts(
    DynamicAmounts.greatestAmongPlayers(
        DynamicAmounts.count(Player.You, Zone.GRAVEYARD),
        Player.EachOpponent
    ),
    ComparisonOperator.GTE,
    8
)

val RelicGolem = card("Relic Golem") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Golem"
    power = 6
    toughness = 6
    oracleText = "This creature can't attack or block unless an opponent has eight or more cards in their graveyard.\n" +
        "{2}, {T}: Target player mills two cards. (They put the top two cards of their library into their graveyard.)"

    staticAbility {
        ability = CantAttackUnless(OpponentHasEightInGraveyard)
    }
    staticAbility {
        ability = CantBlockUnless(OpponentHasEightInGraveyard)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(2, player)
        description = "{2}, {T}: Target player mills two cards."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "249"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b59c75b-d8f8-4f95-9030-ed73c67d988e.jpg?1783929312"
        ruling("2020-09-25", "Once Relic Golem has attacked, causing its first ability to apply by removing cards from graveyards won't cause Relic Golem to be removed from combat.")
        ruling("2020-09-25", "Similarly, once Relic Golem has blocked an attacking creature, causing its first ability to apply won't cause that attacking creature to become unblocked or remove Relic Golem from combat.")
        ruling("2020-09-25", "An ability that offers a bonus if an opponent has eight or more cards in their graveyard won't provide additional benefits if more than one opponent has eight or more cards in their graveyard. There are also no additional benefits no matter how many cards are in an opponent's graveyard, as long as there are at least eight.")
        ruling("2020-09-25", "In a multiplayer game, once an opponent leaves the game, they won't be considered for such an effect, no matter how many cards they had in their graveyard before leaving the game.")
    }
}
