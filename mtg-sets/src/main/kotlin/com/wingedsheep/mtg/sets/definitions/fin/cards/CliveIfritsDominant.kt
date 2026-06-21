package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.ReturnFace
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Clive, Ifrit's Dominant // Ifrit, Warden of Inferno
 * {4}{R}{R} — Legendary Creature — Human Noble Warrior 5/5
 * //  — Legendary Enchantment Creature — Saga Demon 9/9
 *
 * Front — Clive, Ifrit's Dominant:
 *   When Clive enters, you may discard your hand, then draw cards equal to your devotion to red.
 *   {4}{R}{R}, {T}: Exile Clive, then return it to the battlefield transformed under its owner's
 *   control. Activate only as a sorcery.
 *
 * Back — Ifrit, Warden of Inferno (eikon Saga):
 *   (As this Saga enters and after your draw step, add a lore counter.)
 *   I — Lunge — Ifrit fights up to one other target creature.
 *   II, III — Brimstone — Add {R}{R}{R}{R}. If Ifrit has three or more lore counters on it, exile
 *   it, then return it to the battlefield (front face up).
 */
private val IfritWardenOfInferno = card("Ifrit, Warden of Inferno") {
    manaCost = ""
    colorIdentity = "R"
    typeLine = "Legendary Enchantment Creature — Saga Demon"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter.)\n" +
        "I — Lunge — Ifrit fights up to one other target creature.\n" +
        "II, III — Brimstone — Add {R}{R}{R}{R}. If Ifrit has three or more lore counters on it, " +
        "exile it, then return it to the battlefield (front face up)."
    power = 9
    toughness = 9

    // I — Lunge — Ifrit fights up to one other target creature.
    sagaChapter(1) {
        val foe = target("creature", TargetObject(optional = true, filter = TargetFilter.OtherCreature))
        effect = Effects.Fight(EffectTarget.Self, foe)
    }

    // II, III — Brimstone — Add {R}{R}{R}{R}. If Ifrit has three or more lore counters on it,
    // exile it, then return it to the battlefield front face up. Only chapter III meets the
    // lore threshold, so it is the chapter that flips Ifrit back to Clive.
    val brimstone = Effects.Composite(
        Effects.AddMana(Color.RED, 4),
        ConditionalEffect(
            condition = Conditions.SourceCounterCountAtLeast("LORE", 3),
            effect = Effects.ExileAndReturnTransformed(EffectTarget.Self, ReturnFace.FRONT),
        ),
    )
    sagaChapter(2) { effect = brimstone }
    sagaChapter(3) { effect = brimstone }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "133"
        artist = "Nino Is"
        imageUri = "https://cards.scryfall.io/normal/back/9/a/9a069e96-2786-493d-aca8-f70611435dbe.jpg?1748707817"
    }
}

private val CliveIfritsDominantFront = card("Clive, Ifrit's Dominant") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Noble Warrior"
    oracleText = "When Clive enters, you may discard your hand, then draw cards equal to your " +
        "devotion to red. (Each {R} in the mana costs of permanents you control counts toward " +
        "your devotion to red.)\n" +
        "{4}{R}{R}, {T}: Exile Clive, then return it to the battlefield transformed under its " +
        "owner's control. Activate only as a sorcery."
    power = 5
    toughness = 5

    // When Clive enters, you may discard your hand, then draw cards equal to your devotion to red.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(
            Effects.Composite(
                Patterns.Hand.discardHand(),
                Effects.DrawCards(DynamicAmounts.devotionTo(Color.RED)),
            ),
        )
    }

    // {4}{R}{R}, {T}: Exile Clive, then return it transformed. Activate only as a sorcery.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{R}{R}"), Costs.Tap)
        timing = TimingRule.SorcerySpeed
        effect = Effects.ExileAndReturnTransformed()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "133"
        artist = "Nino Is"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a069e96-2786-493d-aca8-f70611435dbe.jpg?1748707817"
    }
}

val CliveIfritsDominant: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = CliveIfritsDominantFront,
    backFace = IfritWardenOfInferno,
)
