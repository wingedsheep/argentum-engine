package com.wingedsheep.mtg.sets.definitions.ody.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Barbarian Ring
 * Land
 * {T}: Add {R}. This land deals 1 damage to you.
 * Threshold — {R}, {T}, Sacrifice this land: It deals 2 damage to any target. Activate only if
 * there are seven or more cards in your graveyard.
 *
 * "Threshold" is an ability word; the ability it labels is a plain activation restriction. The
 * land is sacrificed as a cost, so "It deals 2 damage" uses the land's last-known information as
 * the damage source.
 */
val BarbarianRing = card("Barbarian Ring") {
    typeLine = "Land"
    colorIdentity = "R"
    oracleText = "{T}: Add {R}. This land deals 1 damage to you.\n" +
        "Threshold — {R}, {T}, Sacrifice this land: It deals 2 damage to any target. " +
        "Activate only if there are seven or more cards in your graveyard."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED) then Effects.DealDamage(1, EffectTarget.PlayerRef(Player.You))
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{R}"), Costs.Tap, Costs.SacrificeSelf)
        val t = target(Targets.Any)
        effect = Effects.DealDamage(2, t)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(Conditions.CardsInGraveyardAtLeast(7))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "313"
        artist = "John Avon"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/1809361e-ae1a-4c47-8464-e6496e94d962.jpg?1783945200"
    }
}
