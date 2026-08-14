package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Fearless Swashbuckler — Aetherdrift #204
 * {1}{U}{R} · Creature — Fish Pirate · 3/3
 *
 * Haste
 * Vehicles you control have haste.
 * Whenever you attack, if a Pirate and a Vehicle attacked this combat, draw three cards, then
 * discard two cards.
 *
 * The intervening "if" is two independent existence checks over `attackedThisCombat()`, not one
 * filter. That is what makes the ruling work: a single permanent that is both a Pirate and a
 * Vehicle satisfies both halves, because each half only asks whether *something* matching it
 * attacked. A single conjunctive filter would have demanded two separate attackers.
 *
 * Scoped to permanents you control, which costs nothing in fidelity — only the attacking player
 * declares attackers in a combat (CR 508.1), and "whenever you attack" makes that you.
 */
private val VehiclesYouControl = GameObjectFilter.Any.withSubtype(Subtype.VEHICLE).youControl()

val FearlessSwashbuckler = card("Fearless Swashbuckler") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Fish Pirate"
    power = 3
    toughness = 3
    oracleText = "Haste\n" +
        "Vehicles you control have haste.\n" +
        "Whenever you attack, if a Pirate and a Vehicle attacked this combat, draw three cards, " +
        "then discard two cards."

    keywords(Keyword.HASTE)

    staticAbility {
        ability = GrantKeyword(Keyword.HASTE, GroupFilter(VehiclesYouControl))
    }

    triggeredAbility {
        trigger = Triggers.YouAttack
        triggerCondition = Conditions.All(
            Conditions.YouControlAtLeast(
                1,
                GameObjectFilter.Creature.withSubtype("Pirate").attackedThisCombat()
            ),
            Conditions.YouControlAtLeast(
                1,
                GameObjectFilter.Any.withSubtype(Subtype.VEHICLE).attackedThisCombat()
            ),
        )
        effect = Effects.Composite(
            Effects.DrawCards(3),
            Effects.Discard(2),
        )
        description = "Whenever you attack, if a Pirate and a Vehicle attacked this combat, " +
            "draw three cards, then discard two cards."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "204"
        artist = "Konstantin Porubov"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d86d66b-481f-44ec-86d8-6fc91b52ef38.jpg?1783907859"

        ruling(
            "2025-02-07",
            "If you attack with a single creature that is both a Pirate and a Vehicle, the last " +
                "ability triggers."
        )
    }
}
