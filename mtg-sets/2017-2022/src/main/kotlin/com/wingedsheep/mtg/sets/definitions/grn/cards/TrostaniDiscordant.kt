package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Trostani Discordant — Guilds of Ravnica #208 (canonical printing)
 * {3}{G}{W} · Legendary Creature — Dryad · 1/4
 *
 * Other creatures you control get +1/+1.
 * When Trostani enters, create two 1/1 white Soldier creature tokens with lifelink.
 * At the beginning of your end step, each player gains control of all creatures they own.
 *
 * The end-step reset visits every creature on the battlefield and hands it to its owner
 * (`Player.OwnerOfIterationEntity`); a creature already under its owner's control is left alone —
 * no new control effect, no summoning sickness, no control-change trigger.
 */
val TrostaniDiscordant = card("Trostani Discordant") {
    manaCost = "{3}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Dryad"
    power = 1
    toughness = 4
    oracleText = "Other creatures you control get +1/+1.\n" +
        "When Trostani enters, create two 1/1 white Soldier creature tokens with lifelink.\n" +
        "At the beginning of your end step, each player gains control of all creatures they own."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true)
        )
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            count = 2,
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier"),
            keywords = setOf(Keyword.LIFELINK)
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Creature),
            effect = Effects.GiveControl(
                permanent = EffectTarget.IterationEntity,
                newController = EffectTarget.PlayerRef(Player.OwnerOfIterationEntity)
            )
        )
        description = "At the beginning of your end step, each player gains control of all creatures they own."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "208"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa1190a4-3d7e-4500-991f-e36ec3d1d9dc.jpg?1783934119"
        ruling("2018-10-05", "The owner of a token is the player who created it.")
        ruling(
            "2018-10-05",
            "Because damage remains marked on a creature until it's removed as the turn ends, nonlethal " +
                "damage dealt to other creatures you control may become lethal if Trostani leaves the " +
                "battlefield during that turn."
        )
        ruling(
            "2018-10-05",
            "If a creature has an ability that triggers at the beginning of each end step and Trostani's " +
                "ability causes you to gain control of it, the ability of that creature is still controlled " +
                "by the creature's former controller. If the creature has an ability that triggers at the " +
                "beginning of your end step, that ability doesn't trigger when you gain control of it."
        )
    }
}
