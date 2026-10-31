package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Doom Foretold
 * {2}{W}{B}
 * Enchantment
 *
 * At the beginning of each player's upkeep, that player sacrifices a nonland, nontoken permanent
 * of their choice. If that player can't, they discard a card, they lose 2 life, you draw a card,
 * you gain 2 life, you create a 2/2 white Knight creature token with vigilance, then you
 * sacrifice this enchantment.
 *
 * The Woebringer Demon shape: "if that player can't" reads
 * [DynamicAmounts.permanentsSacrificedThisWay] off the edict that just resolved, so the fallback
 * fires exactly when nothing was sacrificed. "They" is the upkeep's player
 * ([Player.TriggeringPlayer]); "you" is Doom Foretold's controller. Every consequence happens even
 * when the player has no card to discard, and sacrificing Doom Foretold is a no-op if it has left
 * the battlefield or changed control (the sacrifice executor only takes a permanent its
 * controller still controls).
 */
val DoomForetold = card("Doom Foretold") {
    manaCost = "{2}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Enchantment"
    oracleText = "At the beginning of each player's upkeep, that player sacrifices a nonland, " +
        "nontoken permanent of their choice. If that player can't, they discard a card, they " +
        "lose 2 life, you draw a card, you gain 2 life, you create a 2/2 white Knight creature " +
        "token with vigilance, then you sacrifice this enchantment."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        effect = Effects.Sacrifice(
            filter = GameObjectFilter.NonlandPermanent.nontoken(),
            count = 1,
            target = EffectTarget.PlayerRef(Player.TriggeringPlayer)
        ) then
            Effects.If(
                condition = Conditions.CompareAmounts(
                    DynamicAmounts.permanentsSacrificedThisWay(),
                    ComparisonOperator.EQ,
                    0
                ),
                then = Effects.Discard(1, EffectTarget.PlayerRef(Player.TriggeringPlayer)) then
                    Effects.LoseLife(2, EffectTarget.PlayerRef(Player.TriggeringPlayer)) then
                    Effects.DrawCards(1) then
                    Effects.GainLife(2) then
                    Effects.CreateToken(
                        power = 2,
                        toughness = 2,
                        colors = setOf(Color.WHITE),
                        creatureTypes = setOf("Knight"),
                        keywords = setOf(Keyword.VIGILANCE),
                        imageUri = "https://cards.scryfall.io/normal/front/7/0/703e7ecf-3d73-40c1-8cfe-0758778817cf.jpg?1783932483",
                    ) then
                    Effects.SacrificeTarget(EffectTarget.Self)
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "187"
        artist = "Daniel Ljunggren"
        flavorText = "In the end, death usually has its way."
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e76c0c83-3e87-474d-bc72-1677eed32cfa.jpg?1783932598"
        ruling("2019-10-04", "If Doom Foretold leaves the battlefield or changes control while its ability is on the stack, its effect still happens. You just won't be able to sacrifice it if instructed to do so.")
        ruling("2019-10-04", "If the player is unable to sacrifice a permanent and is unable to discard a card, they still lose 2 life, and the rest of Doom Foretold's effects happen.")
        ruling("2019-10-04", "If the player can sacrifice a nonland, nontoken permanent, that player must do so. They can't choose to rush toward their doom.")
        ruling("2019-10-04", "During your upkeep, you can sacrifice Doom Foretold for its own ability.")
    }
}
