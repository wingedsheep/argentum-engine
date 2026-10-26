package com.wingedsheep.mtg.sets.definitions.usg.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Goblin Lackey
 * {R}
 * Creature — Goblin
 * 1/1
 * Whenever this creature deals damage to a player, you may put a Goblin permanent card from your
 * hand onto the battlefield.
 *
 * Any damage to any player (not just combat damage, not just opponents). "Goblin permanent card"
 * is the Goblin subtype on any permanent card — kindred Goblins included, per the 2008 ruling.
 * `putFromHand`'s `ChooseUpTo(1)` carries the "you may".
 */
val GoblinLackey = card("Goblin Lackey") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin"
    power = 1
    toughness = 1
    oracleText = "Whenever this creature deals damage to a player, you may put a Goblin permanent card " +
        "from your hand onto the battlefield."

    triggeredAbility {
        trigger = Triggers.self.dealsDamage(Recipient.AnyPlayer)
        effect = Patterns.Hand.putFromHand(
            filter = GameObjectFilter.Permanent.withSubtype(Subtype.GOBLIN),
            count = 1,
            prompt = "Put a Goblin permanent card onto the battlefield?",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "190"
        artist = "Jerry Tiritilli"
        flavorText = "All bark, someone else's bite."
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9b848caa-aad8-4060-8f86-304a8556de2d.jpg?1783946330"
        ruling(
            "2008-08-01",
            "A Goblin permanent card is an artifact, creature, land, and/or enchantment card with the " +
                "creature type \"Goblin\". Usually such cards will be creatures in addition to any other " +
                "types, but they may be kindred instead. For example, you could put Boggart Shenanigans " +
                "onto the battlefield with this effect."
        )
    }
}
