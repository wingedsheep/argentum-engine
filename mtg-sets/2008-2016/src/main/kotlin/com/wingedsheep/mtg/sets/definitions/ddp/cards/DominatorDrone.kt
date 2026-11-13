package com.wingedsheep.mtg.sets.definitions.ddp.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dominator Drone
 * {2}{B}
 * Creature — Eldrazi Drone
 * 3/2
 * Devoid (This card has no color.)
 * Ingest (Whenever this creature deals combat damage to a player, that player exiles the top card of their library.)
 * When this creature enters, if you control another colorless creature, each opponent loses 2 life.
 *
 * Canonical here: Duel Decks: Zendikar vs. Eldrazi (2015-08-28) printed it five weeks before
 * Battle for Zendikar, which carries a `Printing` row.
 *
 * "If you control another colorless creature" is an intervening "if" (CR 603.4): checked as the
 * creature enters and again on resolution. Colour is read from projected state, so another
 * devoid creature counts.
 */
val DominatorDrone = card("Dominator Drone") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Eldrazi Drone"
    power = 3
    toughness = 2
    oracleText = "Devoid (This card has no color.)\n" +
        "Ingest (Whenever this creature deals combat damage to a player, that player exiles the top card of their library.)\n" +
        "When this creature enters, if you control another colorless creature, each opponent loses 2 life."

    keywords(Keyword.DEVOID, Keyword.INGEST)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.YouControl(
            GameObjectFilter.Creature.withCardPredicate(CardPredicate.IsColorless),
            excludeSelf = true
        )
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "51"
        artist = "James Zapata"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d9f48118-243b-4780-98a3-389f59e1b5b0.jpg?1783938241"
        ruling(
            "2015-08-25",
            "Dominator Drone's last ability checks to see if you control another colorless creature at the moment it enters the battlefield. If you don't, the ability won't trigger at all. If you do, the ability will check again as it tries to resolve. If you no longer control another colorless creature, the ability will be removed from the stack and have no effect."
        )
        ruling("2015-08-25", "The card exiled by the ingest ability is exiled face up.")
        ruling("2015-08-25", "If the player has no cards in their library when the ingest ability resolves, nothing happens. That player won't lose the game (until they have to draw a card from an empty library).")
    }
}
