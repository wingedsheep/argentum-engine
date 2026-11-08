package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Knollspine Dragon
 * {5}{R}{R}
 * Creature — Dragon
 * 7/5
 *
 * Flying
 * When this creature enters, you may discard your hand and draw cards equal to the damage dealt to
 * target opponent this turn.
 *
 * The opponent is chosen when the ability goes on the stack; the "may" and the damage total are
 * both settled on resolution. The total is every point of damage dealt to that opponent this turn
 * (combat or not, from any source), so it is unaffected by discarding the hand first.
 */
val KnollspineDragon = card("Knollspine Dragon") {
    manaCost = "{5}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 7
    toughness = 5
    oracleText = "Flying\n" +
        "When this creature enters, you may discard your hand and draw cards equal to the damage " +
        "dealt to target opponent this turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        target(Targets.Opponent)
        effect = Effects.May(
            Patterns.Hand.discardHand() then
                Effects.DrawCards(DynamicAmounts.damageDealtToTargetPlayerThisTurn())
        )
        description = "When this creature enters, you may discard your hand and draw cards equal to the " +
            "damage dealt to target opponent this turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "98"
        artist = "Steve Prescott"
        flavorText = "It woke to find a vast world in need of a master."
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bfcfec58-bfe4-4b04-80e3-ee11c44e91a7.jpg?1783942747"
        ruling(
            "2008-05-01",
            "You target an opponent when the ability triggers. You don't decide whether you want to discard and draw until the ability resolves."
        )
        ruling(
            "2008-05-01",
            "This ability counts the total amount of damage (both combat and noncombat) dealt to the targeted opponent by all sources (including ones you controlled and ones you didn't) over the course of the turn. Damage that was prevented or replaced doesn't count. Damage that resolved but didn't cause loss of life (due to Worship, for example) will count."
        )
        ruling(
            "2008-05-01",
            "Knollspine Dragon's ability checks only whether damage was dealt. It doesn't care whether the player's life total also changed for other reasons (such as if the player paid life or gained life). For example, if an opponent was dealt 4 damage and gained 6 life during the turn, that player will have a higher life total than they started the turn with — but Knollspine Dragon's ability will let you discard your hand and draw four cards."
        )
    }
}
