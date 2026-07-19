package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ChooseActionEffect
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeEffect

/**
 * K'un-Lun Warrior (MSH #140) — {1}{R} Creature — Human Warrior Hero, 2/2
 *
 * When this creature enters, you may sacrifice an artifact or discard a card. If you do, draw a card.
 *
 * The ETB is Vision of Love's optional sacrifice-or-discard, one card smaller: a [MayEffect]
 * over a [ChooseActionEffect] whose branches each pay their own cost and then draw. The
 * [FeasibilityCheck]s hide an option the controller can't perform, so the draw only ever
 * follows a cost that was actually paid ("If you do").
 */
val KunLunWarrior = card("K'un-Lun Warrior") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior Hero"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, you may sacrifice an artifact or discard a card. " +
        "If you do, draw a card."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(
            effect = ChooseActionEffect(
                choices = listOf(
                    EffectChoice(
                        label = "Sacrifice an artifact",
                        effect = SacrificeEffect(filter = GameObjectFilter.Artifact) then
                            Effects.DrawCards(1),
                        feasibilityCheck = FeasibilityCheck.ControlsPermanentMatching(
                            filter = GameObjectFilter.Artifact
                        ),
                    ),
                    EffectChoice(
                        label = "Discard a card",
                        effect = Patterns.Hand.discardCards(1) then Effects.DrawCards(1),
                        feasibilityCheck = FeasibilityCheck.HasCardsInZone(zone = Zone.HAND),
                    ),
                )
            ),
            descriptionOverride = "You may sacrifice an artifact or discard a card. " +
                "If you do, draw a card.",
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "140"
        artist = "Smirtouille"
        flavorText = "\"The old men of K'un-Lun were afraid to train me. Now I am their last line " +
            "of defense.\"\n—Sparrow, Thunderer of K'un-Lun"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e6e8359d-12be-49be-8cbe-6a3b8506a9e6.jpg?1783902928"
    }
}
