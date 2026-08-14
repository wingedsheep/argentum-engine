package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec

/**
 * Ulvenwald Mysteries
 * {2}{G}
 * Enchantment
 *
 * Whenever a nontoken creature you control dies, investigate.
 * Whenever you sacrifice a Clue, create a 1/1 white Human Soldier creature token.
 *
 * Both halves are ANY-binding, not OTHER: per the SOI rulings Ulvenwald Mysteries reacts to its own
 * death if some effect has made it a creature, and a leaves-the-battlefield trigger looks back in
 * time, so a creature dying *simultaneously* with the enchantment still triggers it.
 *
 * The Clue half uses `perPermanent = true` — "Whenever you sacrifice **a** Clue" is a per-object
 * template (CR 603.2c), so cracking two Clues at once makes two Humans, unlike a batched
 * "one or more" trigger. It is a triggered ability, not an activated one: the sacrifice has to come
 * from somewhere else (typically the Clue's own "{2}, Sacrifice this token: Draw a card").
 */
val UlvenwaldMysteries = card("Ulvenwald Mysteries") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Whenever a nontoken creature you control dies, investigate. (Create a Clue token. " +
        "It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")\n" +
        "Whenever you sacrifice a Clue, create a 1/1 white Human Soldier creature token."

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Creature.youControl().nontoken(),
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.ANY
        )
        effect = Effects.Investigate()
        description = "Whenever a nontoken creature you control dies, investigate."
    }

    triggeredAbility {
        trigger = TriggerSpec(
            event = EventPattern.PermanentsSacrificedEvent(
                filter = GameObjectFilter.Artifact.withSubtype("Clue"),
                perPermanent = true
            ),
            binding = TriggerBinding.ANY
        )
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Human", "Soldier"),
            imageUri = "https://cards.scryfall.io/normal/front/d/9/d9cbf36e-4044-4f08-9bae-f0dcb2455716.jpg?1783937683"
        )
        description = "Whenever you sacrifice a Clue, create a 1/1 white Human Soldier creature token."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "236"
        artist = "Greg Opalinski"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/911a2b5d-7e2d-4358-8e38-cbae7192e4d4.jpg?1783937718"

        ruling(
            "2025-01-24",
            "If a nontoken creature dies at the same time as Ulvenwald Mysteries leaves the " +
                "battlefield, the first ability triggers. Similarly, if Ulvenwald Mysteries isn't a " +
                "token and has become a creature, it dying will cause its own first ability to trigger."
        )
        ruling(
            "2025-01-24",
            "The last ability is a triggered ability, not an activated ability. It doesn't allow you " +
                "to sacrifice a Clue whenever you want; rather, you need some other way of sacrificing " +
                "it, such as the activated ability that Clue tokens have."
        )
        ruling(
            "2025-01-24",
            "If you sacrifice a Clue as part of casting a spell or activating an ability, the last " +
                "ability will resolve before that spell or ability."
        )
    }
}
