package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rowan, Scholar of Sparks // Will, Scholar of Frost — Strixhaven: School of Mages #156
 * (canonical printing)
 * {2}{R} Legendary Planeswalker — Rowan · Loyalty 2 // {4}{U} Legendary Planeswalker — Will ·
 * Loyalty 4
 * Modal double-faced card (CR 712): cast either face; each enters with its own printed loyalty.
 *
 * Both faces share the static "Instant and sorcery spells you cast cost {1} less to cast" —
 * Storm Skreelix's [ModifySpellCost] over instants and sorceries, generic mana only (ruling).
 *
 * Front — Rowan:
 *  - +1: 1 damage to each opponent, or 3 if you've drawn three or more cards this turn — an
 *    `If` over [DynamicAmounts.cardsDrawnThisTurn], checked as the ability resolves.
 *  - −4: an emblem (a global triggered ability, so it outlives Rowan) with Reflective Golem's
 *    trigger body: `MayPay {2}` → copy the triggering spell, retargetable (CR 707.10c). The {2}
 *    can be paid only once per trigger (ruling).
 *
 * Back — Will:
 *  - +1: up to one target creature gets base P/T 0/2 until your next turn (layer 7b, so
 *    modifiers and counters still apply on top — ruling).
 *  - −3: draw two cards.
 *  - −7: exile up to five target permanents as one move, then one 4/4 blue and red Elemental
 *    for each permanent actually exiled, created by that permanent's last-known controller
 *    ([Player.ControllerOfIterationEntity], the Phasing of Zhalfir shape).
 */
private val instantSorceryDiscount = ModifySpellCost(
    target = SpellCostTarget.YouCast(GameObjectFilter.InstantOrSorcery),
    modification = CostModification.ReduceGeneric(1),
)

private const val ROWAN_EMBLEM_TEXT =
    "Whenever you cast an instant or sorcery spell, you may pay {2}. If you do, copy that spell. " +
        "You may choose new targets for the copy."

private val RowanScholarOfSparksFront = card("Rowan, Scholar of Sparks") {
    manaCost = "{2}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Planeswalker — Rowan"
    startingLoyalty = 2
    oracleText = "Instant and sorcery spells you cast cost {1} less to cast.\n" +
        "+1: Rowan deals 1 damage to each opponent. If you've drawn three or more cards this turn, " +
        "she deals 3 damage to each opponent instead.\n" +
        "−4: You get an emblem with \"$ROWAN_EMBLEM_TEXT\""

    staticAbility {
        ability = instantSorceryDiscount
    }

    loyaltyAbility(+1) {
        effect = Effects.If(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.cardsDrawnThisTurn(Player.You),
                ComparisonOperator.GTE,
                3
            ),
            then = Effects.DealDamage(3, EffectTarget.PlayerRef(Player.EachOpponent)),
            otherwise = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent)),
        )
        description = "Rowan deals 1 damage to each opponent. If you've drawn three or more cards " +
            "this turn, she deals 3 damage to each opponent instead."
    }

    loyaltyAbility(-4) {
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
                effect = Effects.MayPay(
                    cost = ManaCost.parse("{2}"),
                    then = Effects.CopyTargetSpell(EffectTarget.TriggeringEntity)
                )
                description = ROWAN_EMBLEM_TEXT
            },
            descriptionOverride = ROWAN_EMBLEM_TEXT
        )
        description = "You get an emblem with \"$ROWAN_EMBLEM_TEXT\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "156"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/18c16872-3675-4a4d-962a-2e17ad6f3886.jpg?1783927332"
        ruling("2021-04-16", "Neither face's first ability can reduce colored mana requirements of spells you cast.")
        ruling("2021-04-16", "To determine the total cost of a spell, start with the mana cost or alternative cost you're paying, add any cost increases, then apply any cost reductions (such as that of Rowan or Will's first ability). The mana value of the spell is determined only by its mana cost, no matter what the total cost to cast the spell was.")
        ruling("2021-04-16", "For Rowan's emblem, you can pay {2} only once. You can't pay more to copy the spell more than once.")
        ruling("2021-04-16", "For Will's first loyalty ability, any effects that modify a creature's power and/or toughness without setting them to a specific value (i.e. ones that don't affect base power and/or toughness) will apply after its base power and toughness are set, regardless of the order those effects were created. The same is true for counters that modify its power and toughness.")
    }
}

private val WillScholarOfFrostBack = card("Will, Scholar of Frost") {
    manaCost = "{4}{U}"
    colorIdentity = "UR"
    typeLine = "Legendary Planeswalker — Will"
    startingLoyalty = 4
    oracleText = "Instant and sorcery spells you cast cost {1} less to cast.\n" +
        "+1: Up to one target creature has base power and toughness 0/2 until your next turn.\n" +
        "−3: Draw two cards.\n" +
        "−7: Exile up to five target permanents. For each permanent exiled this way, its " +
        "controller creates a 4/4 blue and red Elemental creature token."

    staticAbility {
        ability = instantSorceryDiscount
    }

    loyaltyAbility(+1) {
        val t = target(TargetFilter.Creature, optional = true)
        effect = Effects.SetBasePowerAndToughness(0, 2, t, Duration.UntilYourNextTurn)
        description = "Up to one target creature has base power and toughness 0/2 until your next turn."
    }

    loyaltyAbility(-3) {
        effect = Effects.DrawCards(2)
        description = "Draw two cards."
    }

    loyaltyAbility(-7) {
        targets(TargetFilter.Permanent, count = 5, optional = true)
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            val exiled = moveTracked(chosen, CardDestination.ToZone(Zone.EXILE))
            run(
                Effects.ForEachInCollection(
                    exiled,
                    Effects.CreateToken(
                        power = 4,
                        toughness = 4,
                        colors = setOf(Color.BLUE, Color.RED),
                        creatureTypes = setOf("Elemental"),
                        controller = EffectTarget.PlayerRef(Player.ControllerOfIterationEntity),
                    )
                )
            )
        }
        description = "Exile up to five target permanents. For each permanent exiled this way, its " +
            "controller creates a 4/4 blue and red Elemental creature token."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "156"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/back/1/8/18c16872-3675-4a4d-962a-2e17ad6f3886.jpg?1783927332"
    }
}

val RowanScholarOfSparks: CardDefinition = CardDefinition.modalDoubleFacedPermanent(
    frontFace = RowanScholarOfSparksFront,
    backFace = WillScholarOfFrostBack,
)
