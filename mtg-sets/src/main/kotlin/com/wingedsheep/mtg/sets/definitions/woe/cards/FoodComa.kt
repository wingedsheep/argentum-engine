package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Food Coma
 * {3}{W}
 * Enchantment
 *
 * When this enchantment enters, exile target creature an opponent controls until this enchantment
 * leaves the battlefield. Create a Food token.
 *
 * The O-Ring shape (Banishing Light): [Effects.ExileUntilLeaves] on the ETB plus a companion
 * [Triggers.LeavesBattlefield] trigger that returns the linked exile. The Food rides along in the
 * *same* ability, so it shares the target's fate — per the 2024-11-08 Food ruling, if the exile
 * target is illegal as the ability resolves the whole ability is countered and no Food is created.
 * [Effects.Composite] gives that for free; a second triggered ability would not.
 */
val FoodComa = card("Food Coma") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, exile target creature an opponent controls until " +
        "this enchantment leaves the battlefield. Create a Food token. (It's an artifact with " +
        "\"{2}, {T}, Sacrifice this token: You gain 3 life.\")"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val victim = target("target", TargetCreature(filter = TargetFilter.CreatureOpponentControls))
        effect = Effects.Composite(
            Effects.ExileUntilLeaves(victim),
            Effects.CreateFood()
        )
        description = "When this enchantment enters, exile target creature an opponent controls " +
            "until this enchantment leaves the battlefield. Create a Food token."
    }

    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "308"
        artist = "Iris Compiet"
        flavorText = "\"Busy as a bumblesheep\"\n—Faerie expression meaning \"asleep\""
        imageUri = "https://cards.scryfall.io/normal/front/6/1/61ba2aed-3514-4db9-8da0-329620b12f63.jpg?1783915040"

        ruling(
            "2024-11-08",
            "Some spells and abilities that create Food tokens may require targets. If each target " +
                "chosen is an illegal target as that spell or ability tries to resolve, it won't " +
                "resolve. You won't create any Food tokens."
        )
        ruling(
            "2024-11-08",
            "Food is an artifact type. Even though it appears on some creatures, it's never a " +
                "creature type."
        )
    }
}
