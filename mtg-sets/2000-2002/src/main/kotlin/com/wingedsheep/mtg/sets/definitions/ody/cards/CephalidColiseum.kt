package com.wingedsheep.mtg.sets.definitions.ody.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cephalid Coliseum
 * Land
 *
 * {T}: Add {U}. This land deals 1 damage to you.
 * Threshold — {U}, {T}, Sacrifice this land: Target player draws three cards, then discards
 * three cards. Activate only if there are seven or more cards in your graveyard.
 */
val CephalidColiseum = card("Cephalid Coliseum") {
    typeLine = "Land"
    colorIdentity = "U"
    oracleText = "{T}: Add {U}. This land deals 1 damage to you.\n" +
        "Threshold — {U}, {T}, Sacrifice this land: Target player draws three cards, then discards " +
        "three cards. Activate only if there are seven or more cards in your graveyard."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLUE) then Effects.DealDamage(1, EffectTarget.PlayerRef(Player.You))
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U}"), Costs.Tap, Costs.SacrificeSelf)
        val player = target(Targets.Player)
        effect = Effects.DrawCards(3, player) then Effects.Discard(3, player)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(Conditions.CardsInGraveyardAtLeast(7))
        )
        description = "Target player draws three cards, then discards three cards."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "317"
        artist = "John Avon"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d5d74112-7244-4c3f-a5eb-b6be671aefe8.jpg?1783945199"
    }
}
