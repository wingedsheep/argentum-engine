package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shaile, Dean of Radiance // Embrose, Dean of Shadow — Strixhaven: School of Mages #158
 * (canonical printing)
 *
 * A modal double-faced card (CR 712) whose faces are both creatures — the
 * [CardDefinition.modalDoubleFacedPermanent] shape (Kianne, Dean of Substance).
 *
 * Front — Shaile ({1}{W}, 1/1 flying, vigilance): "{T}: +1/+1 counter on each creature that entered
 * the battlefield under your control this turn" reads `enteredThisTurnUnderYourControl()`, the
 * entry log of the player who controls Shaile — so a creature stolen after it entered under your
 * control still gets one (ruling), and one you gained control of doesn't.
 *
 * Back — Embrose ({2}{B}{B}, 4/4): the counter-then-damage tap ability on another target creature,
 * and the Meltstrider Eulogist "creature you control with a +1/+1 counter dies → draw" trigger.
 */
private val ShaileDeanOfRadianceFront = card("Shaile, Dean of Radiance") {
    manaCost = "{1}{W}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Bird Cleric"
    power = 1
    toughness = 1
    oracleText = "Flying, vigilance\n" +
        "{T}: Put a +1/+1 counter on each creature that entered the battlefield under your control this turn."

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.enteredThisTurnUnderYourControl()),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
        )
        description = "{T}: Put a +1/+1 counter on each creature that entered the battlefield under your " +
            "control this turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "158"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/938cee8f-ac2c-49a5-9ff7-1367d0edfabe.jpg?1783927332"
        ruling(
            "2021-04-16",
            "If a creature entered the battlefield under your control this turn but is controlled by a " +
                "different player when Shaile's activated ability resolves, it will still get a +1/+1 counter.",
        )
    }
}

private val EmbroseDeanOfShadowBack = card("Embrose, Dean of Shadow") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Human Warlock"
    power = 4
    toughness = 4
    oracleText = "{T}: Put a +1/+1 counter on another target creature, then Embrose deals 2 damage to that " +
        "creature.\n" +
        "Whenever a creature you control with a +1/+1 counter on it dies, draw a card."

    activatedAbility {
        cost = Costs.Tap
        val creature = target(TargetFilter.OtherCreature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.DealDamage(2, creature)
        description = "{T}: Put a +1/+1 counter on another target creature, then Embrose deals 2 damage to " +
            "that creature."
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl().withCounter(CounterType.PLUS_ONE_PLUS_ONE)).dies()
        effect = Effects.DrawCards(1)
        description = "Whenever a creature you control with a +1/+1 counter on it dies, draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "158"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/back/9/3/938cee8f-ac2c-49a5-9ff7-1367d0edfabe.jpg?1783927332"
        ruling(
            "2021-04-16",
            "If a creature entered the battlefield under your control this turn but is controlled by a " +
                "different player when Shaile's activated ability resolves, it will still get a +1/+1 counter.",
        )
    }
}

val ShaileDeanOfRadiance: CardDefinition = CardDefinition.modalDoubleFacedPermanent(
    frontFace = ShaileDeanOfRadianceFront,
    backFace = EmbroseDeanOfShadowBack,
)
