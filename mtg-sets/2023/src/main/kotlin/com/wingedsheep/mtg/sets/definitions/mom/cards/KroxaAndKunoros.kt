package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Kroxa and Kunoros
 * {3}{R}{W}{B}
 * Legendary Creature — Elder Giant Dog
 * 6/6
 * Vigilance, menace, lifelink
 * Whenever Kroxa and Kunoros enters or attacks, you may exile five cards from your graveyard.
 * When you do, return target creature card from your graveyard to the battlefield.
 *
 * Shape notes:
 *  - "Enters or attacks" is two triggered abilities sharing one effect (Sun Titan convention).
 *  - "When you do" is a genuine CR 603.12 reflexive trigger: the ability goes on the stack without a
 *    target, and the creature card is targeted only after the five cards are exiled (per the
 *    2023-04-14 ruling).
 *  - Exiling five cards is impossible with fewer than five in your graveyard, and the reflexive
 *    half must not pay out off a partial exile. `ReflexiveTriggerEffectExecutor` treats a short
 *    `chooseExactly` collection as feasible (it clamps), so the whole offer is gated on the
 *    graveyard holding at least five cards at resolution — nothing can change the count between
 *    that check and the exile.
 */
private val exileFiveThenReanimate: Effect = Effects.If(
    condition = Conditions.CardsInGraveyardAtLeast(5),
    then = Effects.ReflexiveTrigger(
        action = Effects.Pipeline {
            val graveyardCards = gather(CardSource.FromZone(zone = Zone.GRAVEYARD))
            val toExile = chooseExactly(
                5,
                from = graveyardCards,
                prompt = "Exile five cards from your graveyard",
                selectedLabel = "Exile"
            )
            exile(toExile)
        },
        descriptionOverride = "You may exile five cards from your graveyard. When you do, return " +
            "target creature card from your graveyard to the battlefield."
    ) {
        val creatureCard = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.PutOntoBattlefield(creatureCard)
    },
    descriptionOverride = "You may exile five cards from your graveyard. When you do, return " +
        "target creature card from your graveyard to the battlefield."
)

val KroxaAndKunoros = card("Kroxa and Kunoros") {
    manaCost = "{3}{R}{W}{B}"
    colorIdentity = "RWB"
    typeLine = "Legendary Creature — Elder Giant Dog"
    oracleText = "Vigilance, menace, lifelink\n" +
        "Whenever Kroxa and Kunoros enters or attacks, you may exile five cards from your graveyard. " +
        "When you do, return target creature card from your graveyard to the battlefield."
    power = 6
    toughness = 6

    keywords(Keyword.VIGILANCE, Keyword.MENACE, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = exileFiveThenReanimate
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = exileFiveThenReanimate
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "245"
        artist = "Ignatius Budi"
        flavorText = "When the Invasion Tree reached the Underworld, it found only teeth and fury."
        imageUri = "https://cards.scryfall.io/normal/front/1/3/13e4a233-e40b-4e6a-9863-4d0fc052484c.jpg?1783916942"
        ruling(
            "2023-04-14",
            "The last ability of Kroxa and Kunoros triggers and goes on the stack without a target. " +
                "If you exile five cards from your graveyard, the second \"reflexive\" triggered " +
                "ability will trigger. You'll choose the target creature card for that second ability at that time."
        )
    }
}
