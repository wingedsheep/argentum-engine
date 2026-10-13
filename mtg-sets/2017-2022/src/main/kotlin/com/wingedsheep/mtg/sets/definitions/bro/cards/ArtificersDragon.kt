package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unearth
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Artificer's Dragon
 * {6}
 * Artifact Creature — Dragon
 * 4/4
 * Flying
 * {R}: Artifact creatures you control get +1/+0 until end of turn.
 * Unearth {3}{R}{R}
 *
 * The pumped set is snapshotted as the ability resolves (per ruling), so artifact creatures that
 * arrive later in the turn aren't affected.
 */
val ArtificersDragon = card("Artificer's Dragon") {
    manaCost = "{6}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Dragon"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "{R}: Artifact creatures you control get +1/+0 until end of turn.\n" +
        "Unearth {3}{R}{R} ({3}{R}{R}: Return this card from your graveyard to the battlefield. It gains haste. " +
        "Exile it at the beginning of the next end step or if it would leave the battlefield. Unearth only as a sorcery.)"

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{R}")
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.ArtifactCreature.youControl()),
            Effects.ModifyStats(1, 0, EffectTarget.IterationEntity)
        )
    }

    unearth("{3}{R}{R}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "291"
        artist = "Leon Tukker"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/ccd88d6c-7d3a-45b1-85b4-b390d60cae14.jpg?1783919994"
        ruling("2022-10-14", "The first activated ability affects only artifact creatures you control at the time it resolves. Any artifact creatures that come under your control later in the turn won't be affected.")
        ruling("2022-10-14", "If a permanent returned to the battlefield with unearth would leave the battlefield for any reason, it's exiled instead—unless the spell or ability that's causing the permanent to leave the battlefield is actually trying to exile it!")
    }
}
