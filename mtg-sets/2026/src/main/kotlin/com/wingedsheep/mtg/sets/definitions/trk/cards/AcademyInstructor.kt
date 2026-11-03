package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Academy Instructor
 * {2}{R}
 * Creature — Bolian Officer
 * 0/3
 *
 * This creature gets +1/+0 for each Officer token you control.
 * Whenever you cast a noncreature spell, create a 1/1 red Officer creature token.
 *
 * Nim Shrieker's self-only [GrantDynamicStats], counting Officer *tokens* — the Instructor is an
 * Officer itself but not a token, so it never counts toward its own boost.
 */
val AcademyInstructor = card("Academy Instructor") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Bolian Officer"
    power = 0
    toughness = 3
    oracleText = "This creature gets +1/+0 for each Officer token you control.\n" +
        "Whenever you cast a noncreature spell, create a 1/1 red Officer creature token."

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Any.withSubtype("Officer").token()
            ).count(),
            toughnessBonus = DynamicAmounts.fixed(0)
        )
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = Effects.CreateToken(
            power = 1, toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Officer"),
            imageUri = "https://cards.scryfall.io/normal/front/d/6/d676cbef-8e31-4ae6-8dfe-6b84160f71e0.jpg?1793363893"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "137"
        artist = "Josu Hernaiz"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/660ec8bc-dc03-4b42-84f3-091c19e2a853.jpg?1793188005"
    }
}
