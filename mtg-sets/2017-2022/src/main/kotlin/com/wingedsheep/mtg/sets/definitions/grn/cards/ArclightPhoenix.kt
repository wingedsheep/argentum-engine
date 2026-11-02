package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Arclight Phoenix
 * {3}{R}
 * Creature — Phoenix 3/2
 * Flying, haste
 * At the beginning of combat on your turn, if you've cast three or more instant and sorcery spells
 * this turn, return this card from your graveyard to the battlefield.
 *
 * The trigger functions only from the graveyard (`triggerZone = Zone.GRAVEYARD`); the spell count is
 * an intervening-if, so it is checked both when the step begins and again on resolution.
 */
val ArclightPhoenix = card("Arclight Phoenix") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Phoenix"
    oracleText = "Flying, haste\n" +
        "At the beginning of combat on your turn, if you've cast three or more instant and sorcery " +
        "spells this turn, return this card from your graveyard to the battlefield."
    power = 3
    toughness = 2

    keywords(Keyword.FLYING, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        triggerZone = Zone.GRAVEYARD
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.spellsCastThisTurn(filter = GameObjectFilter.InstantOrSorcery),
            ComparisonOperator.GTE, 3
        )
        effect = Effects.Move(EffectTarget.Self, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
        description = "At the beginning of combat on your turn, if you've cast three or more instant " +
            "and sorcery spells this turn, return this card from your graveyard to the battlefield."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "91"
        artist = "Slawomir Maniak"
        flavorText = "Some storms never blow over."
        imageUri = "https://cards.scryfall.io/normal/front/7/8/787de9ce-02c5-4a17-a88b-d38e83dbeb0b.jpg?1783934168"
        ruling("2018-10-05", "Because the beginning of combat step is before attackers are declared, you can attack with Arclight Phoenix during the same combat it returns to the battlefield.")
    }
}
