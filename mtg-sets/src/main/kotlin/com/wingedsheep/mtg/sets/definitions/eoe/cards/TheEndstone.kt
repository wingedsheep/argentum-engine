package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * The Endstone
 * {7}
 * Legendary Artifact
 * Whenever you play a land or cast a spell, draw a card.
 * At the beginning of your end step, your life total becomes half your starting
 * life total, rounded up.
 */
val TheEndstone = card("The Endstone") {
    manaCost = "{7}"
    colorIdentity = ""
    typeLine = "Legendary Artifact"
    oracleText = "Whenever you play a land or cast a spell, draw a card.\n" +
        "At the beginning of your end step, your life total becomes half your starting life total, rounded up."

    // "Whenever you play a land" — a land moving from hand to battlefield under your control.
    // The hand-zone filter excludes effects that put lands onto the battlefield from other zones
    // (e.g. fetchland search-and-put from library), which would otherwise over-trigger landfall.
    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Land.youControl(),
                from = Zone.HAND,
                to = Zone.BATTLEFIELD,
            ),
            binding = TriggerBinding.OTHER,
        )
        effect = Effects.DrawCards(1)
    }

    triggeredAbility {
        trigger = Triggers.YouCastSpell
        effect = Effects.DrawCards(1)
    }

    triggeredAbility {
        trigger = Triggers.YourEndStep
        effect = Effects.SetLifeTotal(
            amount = DynamicAmount.Divide(
                numerator = DynamicAmount.StartingLifeTotal(Player.You),
                denominator = DynamicAmount.Fixed(2),
                roundUp = true,
            ),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "240"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a451a459-18e0-4c53-a171-3e9da534ebf1.jpg?1752947540"
        ruling(
            "2025-07-25",
            "For your life total to become a certain value, you will gain or lose the appropriate amount of life. " +
                "For example, if your starting life total is 20 and your life total is 17 when The Endstone's last " +
                "ability resolves, you will lose 7 life and your life total will become 10. Other abilities that " +
                "interact with life gain or life loss will interact with this effect accordingly.",
        )
        ruling(
            "2025-07-25",
            "The Endstone's first ability resolves before the spell that caused it to trigger. It resolves even " +
                "if that spell is countered or otherwise leaves the stack without resolving.",
        )
    }
}
