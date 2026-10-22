package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Harvesttide Sentry (Innistrad: Midnight Hunt #186)
 * {1}{G}
 * Creature — Human Warrior
 * 3/1
 * Coven — At the beginning of combat on your turn, if you control three or more creatures with
 * different powers, this creature can't be blocked by creatures with power 2 or less this turn.
 */
val HarvesttideSentry = card("Harvesttide Sentry") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Warrior"
    oracleText = "Coven — At the beginning of combat on your turn, if you control three or more " +
        "creatures with different powers, this creature can't be blocked by creatures with power 2 " +
        "or less this turn."
    power = 3
    toughness = 1

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        interveningIf = Conditions.CompareAmounts(
            left = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Creature,
            ).distinctValues(CardNumericProperty.POWER),
            operator = ComparisonOperator.GTE,
            right = 3,
        )
        effect = Effects.GrantStaticAbility(
            ability = CantBeBlockedBy(GameObjectFilter.Creature.powerAtMost(2)),
            target = EffectTarget.Self
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "186"
        artist = "Livia Prima"
        flavorText = "She patrols the darkness so that others can forget their worries for one bright evening."
        imageUri = "https://cards.scryfall.io/normal/front/9/5/9543cf53-8cf5-49d0-9c60-8b11e21da953.jpg?1783925579"
        ruling("2021-09-24", "Harvesttide Sentry's triggered ability checks your creatures' powers both when it triggers and as it tries to resolve. If you somehow don't have enough creatures with different powers as it tries to resolve, nothing happens and Harvesttide Sentry can be blocked as normal.")
        ruling("2021-09-24", "Once its ability resolves, removing one of the creatures with different powers won't allow a player to block Harvesttide Sentry with creatures with power 2 or less.")
        ruling("2021-09-24", "The blocking restriction is applied only when blockers are declared. After the coven ability resolves, if a player blocks Harvesttide Sentry with a creature with power greater than 2, reducing that creature's power won't make Harvesttide Sentry unblocked.")
        ruling("2021-09-24", "For three creatures to have different powers from one another, each of their powers needs to be different. A 1/1 creature, a 2/1 creature, and another 2/1 creature aren't three creatures with different powers, even though both 2/1 creatures have different power than the 1/1 creature.")
    }
}
