package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.effects.CreatePredefinedTokenEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * U.S.Agent, John Walker — Marvel Super Heroes #236
 * {3}{W/B} · Legendary Creature — Human Soldier Hero · 3/2
 *
 * When U.S.Agent enters, create a colorless Equipment artifact token named Sturdy Shield with
 * "Equipped creature gets +1/+2" and equip {2}. Attach it to U.S.Agent.
 *
 * Modeling notes:
 *  - The Mabel, Heir to Cragflame shape: a named Equipment token whose characteristics live once
 *    in `PredefinedTokens.kt` (`Sturdy Shield` — `ModifyStats(+1, +2, EquippedCreature)` plus
 *    `equipAbility("{2}")`), minted by [CreatePredefinedTokenEffect] rather than re-declared here.
 *  - "Attach it to U.S.Agent" is not an equip activation (no cost, no sorcery timing), so it's the
 *    forced [Effects.AttachTargetEquipmentToCreature]. The token is addressed through the
 *    well-known [CREATED_TOKENS] pipeline collection that the create step publishes, so the attach
 *    lands on *this* Sturdy Shield and not some earlier copy.
 *  - Both halves are one composite under a single ETB trigger, matching the printed single ability.
 *    If U.S.Agent has already left the battlefield when the trigger resolves, the token is still
 *    created and the attach is a graceful no-op.
 */
val UsAgentJohnWalker = card("U.S.Agent, John Walker") {
    manaCost = "{3}{W/B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Human Soldier Hero"
    oracleText = "When U.S.Agent enters, create a colorless Equipment artifact token named " +
        "Sturdy Shield with \"Equipped creature gets +1/+2\" and equip {2}. Attach it to U.S.Agent."
    power = 3
    toughness = 2

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            CreatePredefinedTokenEffect("Sturdy Shield"),
            Effects.AttachTargetEquipmentToCreature(
                equipmentTarget = EffectTarget.PipelineTarget(CREATED_TOKENS, 0),
                creatureTarget = EffectTarget.Self,
            ),
        )
        description = "When U.S.Agent enters, create a colorless Equipment artifact token named " +
            "Sturdy Shield with \"Equipped creature gets +1/+2\" and equip {2}. Attach it to U.S.Agent."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "236"
        artist = "Julia Vasilyeva"
        flavorText = "\"Rogers can talk all he wants about unity and dreams. Enemies understand " +
            "strength.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35e11911-799c-4d82-9109-3ca27964bef0.jpg?1783902894"
    }
}
