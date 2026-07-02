package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Balamb Garden, SeeD Academy // Balamb Garden, Airborne — Final Fantasy #272
 * Land — Town // Legendary Artifact — Vehicle 5/4
 *
 * Front — Balamb Garden, SeeD Academy:
 *   This land enters tapped.
 *   {T}: Add {G} or {U}.
 *   {5}{G}{U}, {T}: Transform this land. This ability costs {1} less to activate for each
 *   other Town you control.
 *
 * Back — Balamb Garden, Airborne:
 *   Flying
 *   Whenever Balamb Garden attacks, draw a card.
 *   Crew 1
 *
 * The transform activation's discount rides
 * [com.wingedsheep.sdk.scripting.ActivatedAbility.genericCostReduction] (Qiqirn Merchant's
 * Town-count pattern) with `excludeSelf = true` for the "each *other* Town" wording. The
 * transform itself is an on-battlefield [TransformEffect] flip — the land turns over into the
 * Vehicle in place (unlike the Dominant exile-and-return cycle, no new object is created).
 */
private val BalambGardenAirborne = card("Balamb Garden, Airborne") {
    manaCost = ""
    colorIdentity = "GU"
    typeLine = "Legendary Artifact — Vehicle"
    oracleText = "Flying\n" +
        "Whenever Balamb Garden attacks, draw a card.\n" +
        "Crew 1 (Tap any number of creatures you control with total power 1 or more: This " +
        "Vehicle becomes an artifact creature until end of turn.)"
    power = 5
    toughness = 4

    keywords(Keyword.FLYING)

    // Whenever Balamb Garden attacks, draw a card.
    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.DrawCards(1)
    }

    keywordAbility(KeywordAbility.crew(1))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "272"
        artist = "Jonas De Ro"
        imageUri = "https://cards.scryfall.io/normal/back/0/0/001e9f20-5b15-41cb-bf82-46172decc235.jpg?1782686387"
    }
}

private val BalambGardenSeedAcademyFront = card("Balamb Garden, SeeD Academy") {
    manaCost = ""
    colorIdentity = "GU"
    typeLine = "Land — Town"
    oracleText = "This land enters tapped.\n" +
        "{T}: Add {G} or {U}.\n" +
        "{5}{G}{U}, {T}: Transform this land. This ability costs {1} less to activate for " +
        "each other Town you control."

    // This land enters tapped.
    replacementEffect(EntersTapped())

    // {T}: Add {G} or {U}.
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    // {5}{G}{U}, {T}: Transform this land. This ability costs {1} less to activate for each
    // other Town you control.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}{G}{U}"), Costs.Tap)
        effect = TransformEffect(EffectTarget.Self)
        genericCostReduction = DynamicAmount.AggregateBattlefield(
            player = Player.You,
            filter = GameObjectFilter.Land.withSubtype("Town"),
            excludeSelf = true,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "272"
        artist = "Jonas De Ro"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/001e9f20-5b15-41cb-bf82-46172decc235.jpg?1782686387"

        ruling(
            "2025-06-06",
            "Town is a land type with no special meaning. It doesn't grant the land any " +
                "intrinsic abilities. Other cards may care about which lands are Towns."
        )
    }
}

val BalambGardenSeedAcademy: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = BalambGardenSeedAcademyFront,
    backFace = BalambGardenAirborne,
)
