package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.readAhead
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Braids's Frightful Return
 * {2}{B}
 * Enchantment — Saga
 *
 * Read ahead
 * I — You may sacrifice a creature. If you do, each opponent discards a card.
 * II — Return target creature card from your graveyard to your hand.
 * III — Target opponent may sacrifice a nonland, nontoken permanent of their choice. If they
 *       don't, they lose 2 life and you draw a card.
 *
 * Chapter I is The Huntsman's Redemption's "you may sacrifice a creature. If you do" shape
 * ([Effects.May] over [Effects.IfYouDo]). Chapter III is the Starseer Mentor punisher —
 * [Effects.PayOrSuffer] asked of the target opponent — whose suffer half runs in the ability
 * controller's context, so the draw goes to the Saga's controller.
 */
val BraidssFrightfulReturn = card("Braids's Frightful Return") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Saga"
    oracleText = "Read ahead (Choose a chapter and start with that many lore counters. Add one " +
        "after your draw step. Skipped chapters don't trigger. Sacrifice after III.)\n" +
        "I — You may sacrifice a creature. If you do, each opponent discards a card.\n" +
        "II — Return target creature card from your graveyard to your hand.\n" +
        "III — Target opponent may sacrifice a nonland, nontoken permanent of their choice. " +
        "If they don't, they lose 2 life and you draw a card."

    readAhead()

    sagaChapter(1) {
        effect = Effects.May(
            effect = Effects.IfYouDo(
                action = Effects.Pipeline {
                    val creatures = gather(GameObjectFilter.Creature, player = Player.You)
                    val chosen = chooseExactly(
                        1,
                        from = creatures,
                        useTargetingUI = true,
                        prompt = "Choose a creature to sacrifice",
                    )
                    sacrifice(chosen)
                },
                then = Patterns.Hand.eachOpponentDiscards(1),
            ),
            descriptionOverride = "You may sacrifice a creature. If you do, each opponent discards a card.",
            feasibility = FeasibilityCheck.ControlsPermanentMatching(GameObjectFilter.Creature.youControl()),
        )
    }

    sagaChapter(2) {
        val creatureCard = target(
            TargetFilter(GameObjectFilter.Creature.ownedByYou(), zone = Zone.GRAVEYARD)
        )
        effect = Effects.Move(creatureCard, Zone.HAND)
    }

    sagaChapter(3) {
        val opponent = target(Targets.Opponent)
        effect = Effects.PayOrSuffer(
            cost = Costs.pay.Sacrifice(filter = GameObjectFilter.Nonland.nontoken()),
            suffer = Effects.LoseLife(amount = 2, target = opponent) then Effects.DrawCards(1),
            player = opponent,
            consequenceDescription = "lose 2 life and let Braids's Frightful Return's controller draw a card",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "85"
        artist = "Dominik Mayer"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49aa8d4c-04e9-4f38-9786-2c9ad735dd72.jpg?1783921335"

        ruling("2022-09-09", "As a Saga with read ahead enters the battlefield, its controller chooses a number from one to that Saga's greatest chapter number. The Saga enters the battlefield with the chosen number of lore counters. Neither choosing the number nor putting the counters on the Saga use the stack, and neither can be responded to.")
    }
}
