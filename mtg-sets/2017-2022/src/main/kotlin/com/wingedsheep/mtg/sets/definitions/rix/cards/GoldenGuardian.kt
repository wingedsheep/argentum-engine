package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Golden Guardian // Gold-Forge Garrison (Rivals of Ixalan #179 — the card's only real printing)
 *
 * Front — Golden Guardian {4}, Artifact Creature — Golem 4/4
 *   Defender
 *   {2}: This creature fights another target creature you control. When this creature dies this
 *   turn, return it to the battlefield transformed under your control.
 *
 * Back — Gold-Forge Garrison, Land
 *   {T}: Add two mana of any one color.
 *   {4}, {T}: Create a 4/4 colorless Golem artifact creature token.
 *
 * Implementation:
 *  - The "dies this turn" rider is a watched-self delayed trigger (War Barge's shape) created on
 *    resolution, so it fires on the Guardian dying from any cause for the rest of the turn, not
 *    only from the fight. It follows the Guardian's current object only: if the Guardian had
 *    already left the battlefield before resolution, it never fires. Its effect is
 *    [Effects.ReturnSelfFromGraveyardTransformed] under the ability controller's control (Journey
 *    to Eternity's shape) — a no-op if the card has left the graveyard, so a second activation's
 *    trigger does nothing once the first has returned it.
 *  - The Golem token's art comes from RIX's token-art row.
 */
private val GoldenGuardianFront = card("Golden Guardian") {
    manaCost = "{4}"
    typeLine = "Artifact Creature — Golem"
    oracleText = "Defender\n" +
        "{2}: This creature fights another target creature you control. When this creature dies " +
        "this turn, return it to the battlefield transformed under your control."
    power = 4
    toughness = 4

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Mana("{2}")
        val other = target(TargetFilter.OtherCreatureYouControl)
        effect = Effects.Fight(EffectTarget.Self, other) then
            Effects.CreateDelayedTrigger(
                effect = Effects.ReturnSelfFromGraveyardTransformed(underYourControl = true),
                trigger = Triggers.self.dies(),
                watchedTarget = EffectTarget.Self,
                expiry = DelayedTriggerExpiry.EndOfTurn,
            )
        description = "{2}: This creature fights another target creature you control. When this " +
            "creature dies this turn, return it to the battlefield transformed under your control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "179"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/3/9/397ba02d-f347-46f7-b028-dd4ba55faa2f.jpg?1783935270"
    }
}

private val GoldForgeGarrison = card("Gold-Forge Garrison") {
    manaCost = ""
    typeLine = "Land"
    oracleText = "(Transforms from Golden Guardian.)\n" +
        "{T}: Add two mana of any one color.\n" +
        "{4}, {T}: Create a 4/4 colorless Golem artifact creature token."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(2)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            creatureTypes = setOf("Golem"),
            artifactToken = true,
        )
        description = "{4}, {T}: Create a 4/4 colorless Golem artifact creature token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "179"
        artist = "Svetlin Velinov"
        flavorText = "An army of living gold for the golden city."
        imageUri = "https://cards.scryfall.io/normal/back/3/9/397ba02d-f347-46f7-b028-dd4ba55faa2f.jpg?1783935270"
    }
}

val GoldenGuardian: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = GoldenGuardianFront,
    backFace = GoldForgeGarrison,
)
