package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Ashcoat of the Shadow Swarm
 * {3}{B}
 * Legendary Creature — Rat Warlock
 * 3/4
 *
 * Whenever Ashcoat attacks or blocks, other Rats you control get +X/+X until end of turn, where X is
 * the number of Rats you control.
 * At the beginning of your end step, you may mill four cards. If you do, return up to two Rat
 * creature cards from your graveyard to your hand.
 *
 * Modeling notes:
 *  - "Attacks or blocks" is two triggered abilities sharing one effect (the Merfolk Skyscout shape).
 *  - "Rats" is a bare tribal noun, so both the pumped group and the count are Rat *permanents*;
 *    the count includes Ashcoat itself, the pump excludes it ("other").
 *  - X is locked as the ability resolves (ruling): `ModifyStatsEffect` evaluates its amount once at
 *    resolution and records a fixed modification, and pumping doesn't change the Rat count mid-loop.
 *  - The return is not targeted: the Rat creature cards are chosen on resolution, after the mill,
 *    so cards milled this way may be returned (ruling). Declining the mill skips the return.
 */
val AshcoatOfTheShadowSwarm = card("Ashcoat of the Shadow Swarm") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Rat Warlock"
    power = 3
    toughness = 4
    oracleText = "Whenever Ashcoat attacks or blocks, other Rats you control get +X/+X until end of turn, " +
        "where X is the number of Rats you control.\n" +
        "At the beginning of your end step, you may mill four cards. If you do, return up to two Rat " +
        "creature cards from your graveyard to your hand. (To mill a card, put the top card of your " +
        "library into your graveyard.)"

    val ratCount = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Permanent.withSubtype("Rat")).count()
    val pumpOtherRats = Patterns.Group.modifyStatsForAll(
        ratCount,
        ratCount,
        GroupFilter(GameObjectFilter.Permanent.withSubtype("Rat").youControl(), excludeSelf = true)
    )

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = pumpOtherRats
    }

    triggeredAbility {
        trigger = Triggers.self.blocks()
        effect = pumpOtherRats
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.May(Effects.Pipeline {
            mill(4)
            val rats = gather(
                CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Creature.withSubtype("Rat"))
            )
            val chosen = chooseUpTo(
                2,
                from = rats,
                showAllCards = true,
                prompt = "Return up to two Rat creature cards from your graveyard to your hand",
                selectedLabel = "Return to hand",
                remainderLabel = "Leave in graveyard"
            )
            toHand(chosen)
        })
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "19"
        artist = "Christina Kraus"
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2fce5989-a2a0-4825-b90a-a1f78659b6e0.jpg?1783919189"
        ruling(
            "2022-12-02",
            "You choose which Rat creature cards to return to your hand as the last ability resolves. " +
                "You may choose Rat creature cards milled with this ability."
        )
        ruling(
            "2022-12-02",
            "The value of X is locked in as the first ability resolves. The bonus it grants won't change " +
                "after that point, even if the number of Rats you control does."
        )
    }
}
