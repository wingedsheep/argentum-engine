package com.wingedsheep.mtg.sets.definitions.ltc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantProtectionFromToGroup
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.Duration

/**
 * Lord of the Nazgûl
 * {3}{U}{B}
 * Legendary Creature — Wraith Noble
 * 4/3
 * Flying
 * Wraiths you control have protection from Ring-bearers.
 * Whenever you cast an instant or sorcery spell, create a 3/3 black Wraith creature token with
 * menace. Then if you control nine or more Wraiths, Wraiths you control have base power and
 * toughness 9/9 until end of turn.
 *
 * "Wraiths" is a bare subtype, so both groups are Wraith *permanents* you control. The 9/9 is a
 * one-shot set-base effect (layer 7b) locked to the Wraiths you control as it resolves (CR 611.2c),
 * so a Wraith that arrives later in the turn keeps its own size.
 */
val LordOfTheNazgul = card("Lord of the Nazgûl") {
    manaCost = "{3}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Wraith Noble"
    power = 4
    toughness = 3
    oracleText = "Flying\n" +
        "Wraiths you control have protection from Ring-bearers.\n" +
        "Whenever you cast an instant or sorcery spell, create a 3/3 black Wraith creature token with " +
        "menace. Then if you control nine or more Wraiths, Wraiths you control have base power and " +
        "toughness 9/9 until end of turn."

    keywords(Keyword.FLYING)

    val wraithsYouControl = GameObjectFilter.Permanent.withSubtype("Wraith").youControl()

    staticAbility {
        ability = GrantProtectionFromToGroup(ProtectionScope.RingBearers, GroupFilter(wraithsYouControl))
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Wraith"),
            keywords = setOf(Keyword.MENACE)
        ) then Effects.If(
            Conditions.YouControlAtLeast(9, GameObjectFilter.Permanent.withSubtype("Wraith")),
            Effects.ForEachInGroup(
                GroupFilter(wraithsYouControl),
                Effects.SetBasePowerAndToughness(
                    power = 9,
                    toughness = 9,
                    target = EffectTarget.IterationEntity,
                    duration = Duration.EndOfTurn
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "60"
        artist = "Anton Solovianchyk"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/41e3d118-8f97-4c68-acff-2f90a055af5b.jpg?1783916019"
    }
}
