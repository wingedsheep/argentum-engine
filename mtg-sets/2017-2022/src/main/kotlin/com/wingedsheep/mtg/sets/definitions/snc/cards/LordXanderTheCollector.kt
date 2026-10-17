package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.div
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lord Xander, the Collector (SNC #197)
 * {4}{U}{B}{R}  Legendary Creature — Vampire Demon Noble  6/6
 *
 * When Lord Xander enters, target opponent discards half the cards in their hand, rounded down.
 * Whenever Lord Xander attacks, defending player mills half their library, rounded down.
 * When Lord Xander dies, target opponent sacrifices half the nonland permanents they control of
 * their choice, rounded down.
 *
 * Each "half … rounded down" is `count / 2` ([DynamicAmount.Divide] with `roundUp = false`), read
 * at resolution off the affected player: the chosen opponent (Rush of Dread's shape, flipped to
 * round down) for the enter and dies triggers, and the defending player for the attack trigger
 * ([TerisianMindbreaker]'s shape, flipped to round down).
 */
val LordXanderTheCollector = card("Lord Xander, the Collector") {
    manaCost = "{4}{U}{B}{R}"
    colorIdentity = "UBR"
    typeLine = "Legendary Creature — Vampire Demon Noble"
    power = 6
    toughness = 6
    oracleText = "When Lord Xander enters, target opponent discards half the cards in their hand, rounded down.\n" +
        "Whenever Lord Xander attacks, defending player mills half their library, rounded down.\n" +
        "When Lord Xander dies, target opponent sacrifices half the nonland permanents they control of " +
        "their choice, rounded down."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.Discard(
            count = DynamicAmounts.zone(opponent.asPlayer, Zone.HAND).count() / 2,
            target = opponent
        )
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Patterns.Library.mill(
            count = DynamicAmounts.zone(Player.DefendingPlayer, Zone.LIBRARY).count() / 2,
            target = EffectTarget.PlayerRef(Player.DefendingPlayer)
        )
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        val opponent = target(Targets.Opponent)
        effect = Effects.Sacrifice(
            filter = GameObjectFilter.NonlandPermanent,
            count = DynamicAmounts.battlefield(opponent.asPlayer, GameObjectFilter.NonlandPermanent).count() / 2,
            target = opponent
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "197"
        artist = "Martina Fačková"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee0a078d-045f-401b-a561-dcb1ad02bf62.jpg?1783923082"
    }
}
