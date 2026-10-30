package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Castle Locthwain
 *
 * Land
 * This land enters tapped unless you control a Swamp.
 * {T}: Add {B}.
 * {1}{B}{B}, {T}: Draw a card, then you lose life equal to the number of cards in your hand.
 *
 * Same entry clause as the rest of the Castle cycle (see [CastleVantress]). The life loss counts
 * the hand at resolution *after* the draw, so the card just drawn is included.
 */
val CastleLocthwain = card("Castle Locthwain") {
    manaCost = ""
    colorIdentity = "B"
    typeLine = "Land"
    oracleText = "This land enters tapped unless you control a Swamp.\n" +
        "{T}: Add {B}.\n" +
        "{1}{B}{B}, {T}: Draw a card, then you lose life equal to the number of cards in your hand."

    replacementEffect(
        EntersTapped(
            unlessCondition = Exists(
                Player.You,
                Zone.BATTLEFIELD,
                GameObjectFilter.Land.withSubtype("Swamp"),
            )
        )
    )

    // {T}: Add {B}.
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    // {1}{B}{B}, {T}: Draw a card, then you lose life equal to the number of cards in your hand.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B}{B}"), Costs.Tap)
        effect = Effects.DrawCards(1) then
            Effects.LoseLife(DynamicAmounts.cardsInYourHand(), EffectTarget.Controller)
        description = "{1}{B}{B}, {T}: Draw a card, then you lose life equal to the number of cards in your hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "241"
        artist = "Titus Lunter"
        flavorText = "Without Locthwain's persistence, the realm would stagnate and die."
        imageUri = "https://cards.scryfall.io/normal/front/1/9/195383c1-4723-40b0-ba53-298dfd8e30d0.jpg?1783932578"
    }
}
