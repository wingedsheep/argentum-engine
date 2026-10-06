package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Gadrak, the Crown-Scourge
 * {2}{R}
 * Legendary Creature — Dragon
 * 5/4
 * Flying
 * Gadrak can't attack unless you control four or more artifacts.
 * At the beginning of your end step, create a Treasure token for each nontoken creature that died
 * this turn.
 *
 * The attack restriction is [CantAttackUnless] over [Conditions.YouControlAtLeast]`(4, Artifact)`
 * (checked only at declaration, so Gadrak stays attacking if artifacts leave afterwards). The
 * Treasure count is game-wide — every player's nontoken creatures, including ones that died before
 * Gadrak entered — so it reads the tracker over [Player.Each].
 */
val GadrakTheCrownScourge = card("Gadrak, the Crown-Scourge") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Dragon"
    power = 5
    toughness = 4
    oracleText = "Flying\n" +
        "Gadrak can't attack unless you control four or more artifacts.\n" +
        "At the beginning of your end step, create a Treasure token for each nontoken creature that died this turn. " +
        "(It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"

    keywords(Keyword.FLYING)

    staticAbility {
        ability = CantAttackUnless(Conditions.YouControlAtLeast(4, GameObjectFilter.Artifact))
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.CreateTreasure(DynamicAmounts.nonTokenCreaturesDiedThisTurn(Player.Each))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "146"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/44b83bb7-00c8-4078-b27f-42a830a544b5.jpg?1783930689"
        ruling("2020-06-23", "Once Gadrak has attacked, it will remain an attacking creature even if you no longer control four or more artifacts.")
        ruling("2020-06-23", "Gadrak's last ability counts creatures that died during the entire turn, even ones that died before Gadrak entered the battlefield.")
    }
}
