package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Hurkyl's Recall
 * {1}{U}
 * Instant
 * Return all artifacts target player owns to their hand.
 *
 * The spell targets a player ([Targets.Player] — either player, including the caster), then
 * bounces every artifact that player *owns* (not merely controls) back to that player's hand.
 * The owner-axis match is the [GameObjectFilter.ownedByTargetPlayer] predicate, so an artifact
 * the target owns but another player controls (after a control-changing effect) is still
 * returned — to its owner, the target. Because [GroupPatterns.returnAllToHand] gathers with
 * `BattlefieldMatching(player = Player.Each)`, no `youControl` constraint is added: the filter
 * matches across the whole battlefield purely on ownership.
 */
val HurkylsRecall = card("Hurkyl's Recall") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Return all artifacts target player owns to their hand."

    spell {
        target("target player", Targets.Player)
        effect = Patterns.Group.returnAllToHand(
            GroupFilter(GameObjectFilter.Artifact.ownedByTargetPlayer())
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "10"
        artist = "NéNé Thomas"
        flavorText = "This spell, attributed to Drafna, was actually the work of his wife Hurkyl."
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f32373dd-06d8-45d1-8777-3b1411bcb30a.jpg?1562946376"
    }
}
