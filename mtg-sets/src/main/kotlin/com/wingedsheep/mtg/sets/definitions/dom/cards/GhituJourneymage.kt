package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Ghitu Journeymage
 * {2}{R}
 * Creature — Human Wizard
 * 3/2
 * When Ghitu Journeymage enters the battlefield, if you control another Wizard,
 * Ghitu Journeymage deals 2 damage to each opponent.
 */
val GhituJourneymage = card("Ghitu Journeymage") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Wizard"
    power = 3
    toughness = 2
    oracleText = "When Ghitu Journeymage enters the battlefield, if you control another Wizard, Ghitu Journeymage deals 2 damage to each opponent."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        // "If you control another Wizard" — count Wizards OTHER than Ghitu Journeymage itself and
        // require at least one. Counting all Wizards (including self) >= 2 would be wrong whenever
        // Ghitu Journeymage is not a Wizard when the intervening-if is checked (CR 603.4) — e.g. a
        // type-changing effect strips its subtypes, or it entered as a non-Wizard.
        triggerCondition = Compare(
            DynamicAmount.AggregateBattlefield(
                Player.You,
                GameObjectFilter.Creature.withSubtype("Wizard"),
                excludeSelf = true
            ),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(1)
        )
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "126"
        artist = "Jason Rainville"
        flavorText = "\"The Ghitu of Shiv are as fierce and uncompromising as their volcanic home.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e4360bc9-76f0-4e82-8968-4c6c62a35ed1.jpg?1562744427"
    }
}
