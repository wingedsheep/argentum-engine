package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Conqueror's Galleon // Conqueror's Foothold (Ixalan #234)
 *
 * Front — Conqueror's Galleon ({4}, Artifact — Vehicle, 2/10)
 *   When this Vehicle attacks, exile it at end of combat, then return it to the battlefield
 *   transformed under your control.
 *   Crew 4
 *
 * Back — Conqueror's Foothold (Land)
 *   {T}: Add {C}.
 *   {2}, {T}: Draw a card, then discard a card.
 *   {4}, {T}: Draw a card.
 *   {6}, {T}: Return target card from your graveyard to your hand.
 *
 * The attack trigger sets up an end-of-combat delayed trigger
 * ([Effects.CreateDelayedTrigger] at [Step.END_COMBAT]) whose effect is
 * [Effects.ExileAndReturnTransformed] with `underYourControl = true`: the Galleon re-enters as a
 * new object on its land face under the trigger's controller, even if they don't own it.
 */
private val ConquerorsGalleonFront = card("Conqueror's Galleon") {
    manaCost = "{4}"
    typeLine = "Artifact — Vehicle"
    power = 2
    toughness = 10
    oracleText = "When this Vehicle attacks, exile it at end of combat, then return it to the " +
        "battlefield transformed under your control.\n" +
        "Crew 4 (Tap any number of creatures you control with total power 4 or more: This " +
        "Vehicle becomes an artifact creature until end of turn.)"

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.CreateDelayedTrigger(
            step = Step.END_COMBAT,
            effect = Effects.ExileAndReturnTransformed(EffectTarget.Self, underYourControl = true),
        )
    }

    keywordAbility(KeywordAbility.crew(4))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "234"
        artist = "Emrah Elmasli"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02bb4b2a-3c9f-48ab-b2a5-ae31f06b82d9.jpg?1783935713"
    }
}

private val ConquerorsFoothold = card("Conqueror's Foothold") {
    manaCost = ""
    typeLine = "Land"
    oracleText = "(Transforms from Conqueror's Galleon.)\n" +
        "{T}: Add {C}.\n" +
        "{2}, {T}: Draw a card, then discard a card.\n" +
        "{4}, {T}: Draw a card.\n" +
        "{6}, {T}: Return target card from your graveyard to your hand."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = Patterns.Hand.loot()
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        effect = Effects.DrawCards(1)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{6}"), Costs.Tap)
        val t = target(TargetFilter.CardInGraveyard.ownedByYou())
        effect = Effects.Move(t, Zone.HAND)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "234"
        artist = "Emrah Elmasli"
        imageUri = "https://cards.scryfall.io/normal/back/0/2/02bb4b2a-3c9f-48ab-b2a5-ae31f06b82d9.jpg?1783935713"
    }
}

val ConquerorsGalleon: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = ConquerorsGalleonFront,
    backFace = ConquerorsFoothold,
)
