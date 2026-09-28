package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ReturnFace
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Elesh Norn // The Argent Etchings — March of the Machine #12
 * {2}{W}{W} · Legendary Creature — Phyrexian Praetor 3/5 // Enchantment — Saga
 *
 * Front: the damage punisher is two triggers because the oracle text spans two recipient kinds —
 * "you" (`Recipient.You`, the damage-to-you bucket) and "a permanent you control". Each fires once
 * per damaged recipient per source, exactly as the 2023-04-14 rulings say; the observer detectors
 * route each ability to its own bucket so a permanent holding both never doubles the You trigger.
 * "That source's controller" is `Player.ControllerOfTriggeringEntity` (the damage source), the form that
 * reads last-known control when a burn spell has already left the stack by resolution (CR 608.2h).
 */
private val TheArgentEtchings = card("The Argent Etchings") {
    manaCost = ""
    colorIndicator = "W"
    colorIdentity = "W"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter.)\n" +
        "I — Incubate 2 five times, then transform all Incubator tokens you control.\n" +
        "II — Creatures you control get +1/+1 and gain double strike until end of turn.\n" +
        "III — Destroy all other permanents except for artifacts, lands, and Phyrexians. Exile this " +
        "Saga, then return it to the battlefield (front face up)."

    sagaChapter(1) {
        effect = Effects.Repeat(DynamicAmounts.fixed(5), Patterns.Mechanic.incubate(2)) then
            Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Permanent.withSubtype("Incubator").token().youControl()),
                Effects.Transform(EffectTarget.IterationEntity)
            )
    }

    sagaChapter(2) {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.ModifyStats(1, 1, EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, EffectTarget.IterationEntity)
        )
    }

    sagaChapter(3) {
        effect = Effects.DestroyAll(
            GameObjectFilter.NonlandPermanent.nonartifact().notSubtype(Subtype.PHYREXIAN).notSourceItself()
        ) then Effects.ExileAndReturnTransformed(EffectTarget.Self, ReturnFace.FRONT)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "12"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/back/d/8/d8999135-ddb1-4e4c-b885-e25f23dac3d3.jpg?1783917075"
    }
}

private val EleshNornFront = card("Elesh Norn") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Phyrexian Praetor"
    power = 3
    toughness = 5
    oracleText = "Vigilance\n" +
        "Whenever a source an opponent controls deals damage to you or a permanent you control, " +
        "that source's controller loses 2 life unless they pay {1}.\n" +
        "{2}{W}, Sacrifice three other creatures: Exile Elesh Norn, then return it to the " +
        "battlefield transformed under its owner's control. Activate only as a sorcery."
    keywords(Keyword.VIGILANCE)

    val sourceController = EffectTarget.PlayerRef(Player.ControllerOfTriggeringEntity)
    val punish = Effects.PayOrSuffer(
        cost = Costs.pay.Mana("{1}"),
        suffer = Effects.LoseLife(2, sourceController),
        player = sourceController
    )

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Any.opponentControls()).dealsDamage(Recipient.You)
        effect = punish
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Any.opponentControls()).dealsDamage(Recipient.PermanentYouControl)
        effect = punish
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{W}"),
            Costs.SacrificeMultiple(3, GameObjectFilter.Creature.notSourceItself())
        )
        timing = TimingRule.SorcerySpeed
        effect = Effects.ExileAndReturnTransformed()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "12"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d8999135-ddb1-4e4c-b885-e25f23dac3d3.jpg?1783917075"
        ruling(
            "2023-04-14",
            "Elesh Norn's triggered ability will trigger separately for each permanent dealt damage by a " +
                "source an opponent controls, plus once if you're dealt damage by that source."
        )
        ruling(
            "2023-04-14",
            "Similarly, the triggered ability will trigger separately for each source an opponent controls " +
                "that deals damage to you or a permanent you control."
        )
    }
}

val EleshNorn: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = EleshNornFront,
    backFace = TheArgentEtchings,
)
