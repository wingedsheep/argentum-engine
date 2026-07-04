package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Kefka, Court Mage // Kefka, Ruler of Ruin
 * {2}{U}{B}{R} — Legendary Creature — Human Wizard 4/5
 *   // Legendary Creature — Avatar Wizard 5/7
 *
 * Front — Kefka, Court Mage:
 *   Whenever Kefka enters or attacks, each player discards a card. Then you draw a card for each
 *   card type among cards discarded this way.
 *   {8}: Each opponent sacrifices a permanent of their choice. Transform Kefka. Activate only as
 *   a sorcery.
 *
 * Back — Kefka, Ruler of Ruin:
 *   Flying
 *   Whenever an opponent loses life during your turn, you draw that many cards.
 *
 * Modeling notes:
 *  - "enters or attacks" has no combined trigger in the engine — it is two sibling triggered
 *    abilities sharing one body (the Sephiroth / Gilgamesh shape).
 *  - "each player discards a card. Then you draw a card for each card type among cards discarded
 *    this way." — the discards are collected into per-player pipeline collections and the payoff
 *    counts distinct card types across them via [DynamicAmount.DistinctCardTypesInCollections].
 *    ForEach iterations get *fresh* stored collections, so "each player" can't accumulate into one
 *    collection inside a loop; like the existing `Patterns.Hand.eachOpponentDiscards(count,
 *    controllerDrawsPerDiscard)` (Syphon Mind), this discards the controller's card plus one
 *    opponent's card — exact in a two-player game (the engine's supported mode) and reaching a
 *    single opponent in multiplayer. The card types are read by entity id so counting still works
 *    after the cards land in graveyards. A discarded artifact creature counts for two types; a
 *    player with an empty hand contributes none (matching "each player discards a card").
 *  - The {8} ability is a mana-only, sorcery-speed activated ability. Each opponent sacrifices a
 *    permanent of *their* choice (edict — the sacrificing player chooses). Transform targets Self,
 *    so a dead Kefka simply doesn't transform.
 *  - Back "an opponent loses life during your turn" = [Triggers.AnOpponentLosesLife] gated by
 *    `triggerCondition = Conditions.IsYourTurn` (fire-time only), drawing life-lost-many cards.
 */
private val KefkaRulerOfRuin = card("Kefka, Ruler of Ruin") {
    manaCost = ""
    colorIdentity = "UBR"
    typeLine = "Legendary Creature — Avatar Wizard"
    oracleText = "Flying\n" +
        "Whenever an opponent loses life during your turn, you draw that many cards."
    power = 5
    toughness = 7

    keywords(Keyword.FLYING)

    // Whenever an opponent loses life during your turn, you draw that many cards.
    triggeredAbility {
        trigger = Triggers.AnOpponentLosesLife
        triggerCondition = Conditions.IsYourTurn
        effect = Effects.DrawCards(
            DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_LIFE_LOST)
        )
        description = "Whenever an opponent loses life during your turn, you draw that many cards."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "231"
        artist = "Xui Ton"
        imageUri = "https://cards.scryfall.io/normal/back/8/f/8fcf3fbb-1ddd-437e-81c1-f5a3133f5ee8.jpg?1782686419"
    }
}

private val KefkaCourtMageFrontFace = card("Kefka, Court Mage") {
    manaCost = "{2}{U}{B}{R}"
    colorIdentity = "UBR"
    typeLine = "Legendary Creature — Human Wizard"
    oracleText = "Whenever Kefka enters or attacks, each player discards a card. Then you draw a " +
        "card for each card type among cards discarded this way.\n" +
        "{8}: Each opponent sacrifices a permanent of their choice. Transform Kefka. Activate " +
        "only as a sorcery."
    power = 4
    toughness = 5

    // Each player discards a card; then you draw a card for each card type among those discards.
    val discardAndDraw = Effects.Composite(
        listOf(
            // You discard a card.
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.HAND, Player.You),
                storeAs = "kefkaSelfHand"
            ),
            SelectFromCollectionEffect(
                from = "kefkaSelfHand",
                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                chooser = Chooser.Controller,
                storeSelected = "kefkaSelfDiscard",
                prompt = "Choose a card to discard"
            ),
            MoveCollectionEffect(
                from = "kefkaSelfDiscard",
                destination = CardDestination.ToZone(Zone.GRAVEYARD),
                moveType = MoveType.Discard
            ),
            // Each opponent discards a card (a single opponent in a two-player game).
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.HAND, Player.AnOpponent),
                storeAs = "kefkaOppHand"
            ),
            SelectFromCollectionEffect(
                from = "kefkaOppHand",
                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                chooser = Chooser.Opponent,
                storeSelected = "kefkaOppDiscard",
                prompt = "Choose a card to discard"
            ),
            MoveCollectionEffect(
                from = "kefkaOppDiscard",
                destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.AnOpponent),
                moveType = MoveType.Discard
            ),
            // Then you draw a card for each card type among cards discarded this way.
            Effects.DrawCards(
                DynamicAmounts.distinctCardTypesIn("kefkaSelfDiscard", "kefkaOppDiscard")
            )
        )
    )

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = discardAndDraw
        description = "Whenever Kefka enters, each player discards a card. Then you draw a card " +
            "for each card type among cards discarded this way."
    }
    triggeredAbility {
        trigger = Triggers.Attacks
        effect = discardAndDraw
        description = "Whenever Kefka attacks, each player discards a card. Then you draw a card " +
            "for each card type among cards discarded this way."
    }

    // {8}: Each opponent sacrifices a permanent of their choice. Transform Kefka. Sorcery speed.
    activatedAbility {
        cost = Costs.Mana("{8}")
        timing = TimingRule.SorcerySpeed
        effect = Effects.Composite(
            listOf(
                Effects.Sacrifice(
                    GameObjectFilter.Any,
                    1,
                    EffectTarget.PlayerRef(Player.EachOpponent)
                ),
                TransformEffect(EffectTarget.Self)
            )
        )
        description = "{8}: Each opponent sacrifices a permanent of their choice. Transform " +
            "Kefka. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "231"
        artist = "Xui Ton"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8fcf3fbb-1ddd-437e-81c1-f5a3133f5ee8.jpg?1782686419"
        ruling(
            "2025-06-06",
            "The card types in Magic include artifact, battle, creature, enchantment, instant, " +
                "kindred, land, planeswalker, and sorcery. Legendary, basic, and snow are " +
                "supertypes, not card types; Hero and Saga are subtypes, not card types."
        )
        ruling(
            "2025-06-06",
            "While resolving Kefka, Court Mage's last ability, the next opponent in turn order " +
                "chooses a permanent they control, then each other opponent in turn order does " +
                "the same. Then each of the chosen permanents are sacrificed simultaneously."
        )
    }
}

val KefkaCourtMage: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = KefkaCourtMageFrontFace,
    backFace = KefkaRulerOfRuin,
)
