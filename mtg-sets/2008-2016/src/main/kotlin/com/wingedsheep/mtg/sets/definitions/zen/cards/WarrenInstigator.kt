package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Warren Instigator
 * {R}{R}
 * Creature — Goblin Berserker
 * 1/1
 * Double strike
 * Whenever this creature deals damage to an opponent, you may put a Goblin creature card from your
 * hand onto the battlefield.
 *
 * Any damage, not only combat damage; with double strike an unblocked Instigator triggers once in
 * each combat damage step. `putFromHand`'s `ChooseUpTo(1)` carries the "you may".
 */
val WarrenInstigator = card("Warren Instigator") {
    manaCost = "{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Berserker"
    power = 1
    toughness = 1
    oracleText = "Double strike\n" +
        "Whenever this creature deals damage to an opponent, you may put a Goblin creature card from " +
        "your hand onto the battlefield."

    keywords(Keyword.DOUBLE_STRIKE)

    triggeredAbility {
        trigger = Triggers.self.dealsDamage(Recipient.Opponent)
        effect = Patterns.Hand.putFromHand(
            filter = GameObjectFilter.Creature.withSubtype(Subtype.GOBLIN),
            count = 1,
            prompt = "Put a Goblin creature card onto the battlefield?",
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "154"
        artist = "Andrew Robinson"
        flavorText = "\"Danger! Danger! Come out of the safety of your holes!\""
        imageUri = "https://cards.scryfall.io/normal/front/9/8/98637219-40d8-414c-939d-e2c90b909725.jpg?1783942138"
        ruling(
            "2009-10-01",
            "Warren Instigator's ability triggers when it deals any kind of damage to an opponent, not " +
                "just combat damage."
        )
        ruling(
            "2009-10-01",
            "If Warren Instigator attacks an opponent and isn't blocked, its double strike ability will " +
                "cause it to deal combat damage to that opponent twice, once during each combat damage " +
                "step. Its triggered ability will thus trigger twice."
        )
    }
}
