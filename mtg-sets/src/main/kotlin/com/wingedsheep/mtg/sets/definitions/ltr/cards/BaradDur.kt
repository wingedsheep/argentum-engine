package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Barad-dûr
 * Legendary Land
 *
 * Barad-dûr enters tapped unless you control a legendary creature.
 * {T}: Add {B}.
 * {X}{X}{B}, {T}: Amass Orcs X. Activate only if a creature died this turn.
 *
 * Gap 25 ({X} in an activated-ability cost) is engine-landed (Atalya, Soul Burn; the X-cost
 * activation continuation threads the chosen X into `DynamicAmount.XValue`). The enters-tapped clause
 * and the died-this-turn activation gate both reuse existing primitives (cf. Mines of Moria).
 */
val BaradDur = card("Barad-dûr") {
    typeLine = "Legendary Land"
    colorIdentity = "B"
    oracleText = "Barad-dûr enters tapped unless you control a legendary creature.\n" +
        "{T}: Add {B}.\n" +
        "{X}{X}{B}, {T}: Amass Orcs X. Activate only if a creature died this turn."

    replacementEffect(
        EntersTapped(
            unlessCondition = Exists(
                player = Player.You,
                zone = Zone.BATTLEFIELD,
                filter = GameObjectFilter.Creature.legendary()
            )
        )
    )

    // {T}: Add {B}.
    activatedAbility {
        cost = Costs.Tap
        effect = AddManaEffect(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    // {X}{X}{B}, {T}: Amass Orcs X. Activate only if a creature died this turn.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}{X}{B}"), Costs.Tap)
        effect = Effects.Amass(DynamicAmount.XValue, "Orc")
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.CreatureDiedThisTurn))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "253"
        artist = "Sean Vo"
        flavorText = "\"Those who pass the gates of Barad-dûr do not return.\"\n—Gandalf"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb5038af-06b0-401e-8dea-a1a8483788ae.jpg?1686970323"
    }
}
