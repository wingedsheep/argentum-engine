package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hazezon Tamar — Legends #230
 * {4}{R}{G}{W} · Legendary Creature — Human Warrior · 2/4
 *
 * When Hazezon enters, create X 1/1 Sand Warrior creature tokens that are red, green, and white
 * at the beginning of your next upkeep, where X is the number of lands you control at that time.
 * When Hazezon leaves the battlefield, exile all Sand Warriors.
 *
 * The enters trigger only schedules a step-based delayed trigger on its controller's next upkeep
 * (`fireOnPlayer = You`); the delayed trigger is independent of Hazezon, so it still fires if
 * Hazezon has left by then, and the player who put Hazezon onto the battlefield gets the tokens
 * (ruling). X is a dynamic count read when the delayed trigger resolves — "at that time".
 *
 * The leaves trigger exiles every permanent with both the Sand and Warrior creature types, any
 * controller, not just the tokens Hazezon made (ruling). Sand Warriors created after it resolves
 * stay.
 */
val HazezonTamar = card("Hazezon Tamar") {
    manaCost = "{4}{R}{G}{W}"
    colorIdentity = "GRW"
    typeLine = "Legendary Creature — Human Warrior"
    power = 2
    toughness = 4
    oracleText = "When Hazezon enters, create X 1/1 Sand Warrior creature tokens that are red, green, " +
        "and white at the beginning of your next upkeep, where X is the number of lands you control " +
        "at that time.\nWhen Hazezon leaves the battlefield, exile all Sand Warriors."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateDelayedTrigger(
            step = Step.UPKEEP,
            fireOnPlayer = EffectTarget.PlayerRef(Player.You),
            effect = Effects.CreateToken(
                count = DynamicAmounts.landsYouControl(),
                power = 1,
                toughness = 1,
                colors = setOf(Color.RED, Color.GREEN, Color.WHITE),
                creatureTypes = setOf("Sand", "Warrior"),
                imageUri = "https://cards.scryfall.io/normal/front/1/7/17812f4e-a10c-472b-970f-35a8f392235a.jpg?1783911123"
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Permanent.withSubtype("Sand").withSubtype("Warrior")),
            effect = Effects.Move(EffectTarget.IterationEntity, Zone.EXILE)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "230"
        artist = "Richard Kane Ferguson"
        imageUri = "https://cards.scryfall.io/normal/front/1/7/17fc3a85-c6b9-4fd2-a6a2-d3210708e5ea.jpg?1783948039"
        ruling(
            "2009-10-01",
            "When Hazezon Tamar's second ability resolves, all permanents with both the Sand and Warrior " +
                "creature types are exiled, not just the Sand Warrior creature tokens it created. Permanents " +
                "that have just one of those types are unaffected."
        )
        ruling(
            "2009-10-01",
            "The first ability creates a delayed triggered ability that triggers at the beginning of your " +
                "next upkeep. It doesn't matter who controls Hazezon Tamar at that time, or even if it's on " +
                "the battlefield. The player who put Hazezon onto the battlefield gets the Sand Warriors."
        )
        ruling(
            "2009-10-01",
            "Only Sand Warriors that are actually on the battlefield when Hazezon Tamar leaves the " +
                "battlefield are affected when its second ability resolves. If Hazezon leaves the battlefield " +
                "after the delayed triggered ability created by its first ability triggers but before that " +
                "ability resolves, its second ability triggers and resolves first, then the Sand Warriors are " +
                "put on the battlefield. Those Sand Warriors will remain on the battlefield indefinitely."
        )
    }
}
