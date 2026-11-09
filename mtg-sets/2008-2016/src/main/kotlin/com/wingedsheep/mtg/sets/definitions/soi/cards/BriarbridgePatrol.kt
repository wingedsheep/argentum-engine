package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Briarbridge Patrol — Shadows over Innistrad #195
 * {3}{G} · Creature — Human Warrior · 3/3 · Uncommon
 *
 * Whenever this creature deals damage to one or more creatures, investigate.
 * At the beginning of each end step, if you sacrificed three or more Clues this turn, you may put
 * a creature card from your hand onto the battlefield.
 *
 * The first ability is the source-side batch: blocked by two creatures, it investigates once. The
 * end-step check counts every Clue you sacrificed this turn as it was when sacrificed, so the
 * Clue tokens that ceased to exist still count.
 */
val BriarbridgePatrol = card("Briarbridge Patrol") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Warrior"
    power = 3
    toughness = 3
    oracleText = "Whenever this creature deals damage to one or more creatures, investigate. " +
        "(Create a Clue token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")\n" +
        "At the beginning of each end step, if you sacrificed three or more Clues this turn, you may " +
        "put a creature card from your hand onto the battlefield."

    triggeredAbility {
        trigger = Triggers.self.dealsDamage(Recipient.AnyCreature, batch = true)
        effect = Effects.Investigate()
        description = "Whenever this creature deals damage to one or more creatures, investigate."
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        interveningIf = Conditions.YouSacrificedThisTurn(GameObjectFilter.Artifact.withSubtype("Clue"), atLeast = 3)
        effect = Patterns.Hand.putFromHand(
            filter = GameObjectFilter.Creature,
            prompt = "Put a creature card from your hand onto the battlefield?",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "195"
        artist = "Greg Opalinski"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f44a017b-40cb-4c15-b9ea-a3ed904e4e49.jpg?1783937736"
    }
}
