package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bruna, Light of Alabaster — Avacyn Restored #208
 * {3}{W}{W}{U} · Legendary Creature — Angel · 5/5
 *
 * Flying, vigilance
 * Whenever Bruna attacks or blocks, you may attach to it any number of Auras on the battlefield and
 * you may put onto the battlefield attached to it any number of Aura cards that could enchant it from
 * your graveyard and/or hand.
 *
 * "Attacks or blocks" is two triggered abilities sharing one effect (a creature never does both in
 * one combat — Mardu Blazebringer's shape). The effect runs in printed order:
 *  1. Any number (0..all) of the Auras on the battlefield, whoever controls them, are chosen and each
 *     attached to Bruna (Armed and Armored's shape). An Aura that couldn't legally enchant Bruna
 *     stays where it is — the attach executor checks the Aura's enchant restriction (first ruling).
 *  2. Any number of Aura cards from your hand and graveyard that could enchant Bruna are chosen and
 *     put onto the battlefield attached to it (Danitha, Benalia's Hope's move with `attachTo = Self`,
 *     narrowed by `couldAttachTo(Self)` as the Oracle text says). If Bruna has left the battlefield,
 *     the Auras stay in their zones.
 * Choosing nothing at either step is the "you may".
 */
private val brunaAttachAuras = Effects.Pipeline {
    val onBattlefield = gather(GameObjectFilter.Any.withSubtype(Subtype.AURA))
    val moving = chooseAnyNumber(
        from = onBattlefield,
        useTargetingUI = true,
        prompt = "Choose any number of Auras on the battlefield to attach to Bruna",
    )
    run(
        Effects.ForEachInCollection(
            moving,
            Effects.AttachTargetEquipmentToCreature(
                equipmentTarget = EffectTarget.IterationEntity,
                creatureTarget = EffectTarget.Self,
            )
        )
    )

    val fromHandAndGraveyard = gather(
        CardSource.FromMultipleZones(
            listOf(Zone.HAND, Zone.GRAVEYARD),
            Player.You,
            GameObjectFilter.Any.withSubtype(Subtype.AURA).couldAttachTo(EffectTarget.Self)
        )
    )
    val putting = chooseAnyNumber(
        from = fromHandAndGraveyard,
        prompt = "Choose any number of Aura cards from your hand and graveyard to put onto the " +
            "battlefield attached to Bruna",
    )
    move(putting, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You), attachTo = EffectTarget.Self)
}

private const val BRUNA_TRIGGER_TEXT = "you may attach to it any number of Auras on the battlefield " +
    "and you may put onto the battlefield attached to it any number of Aura cards that could enchant " +
    "it from your graveyard and/or hand."

val BrunaLightOfAlabaster = card("Bruna, Light of Alabaster") {
    manaCost = "{3}{W}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Angel"
    power = 5
    toughness = 5
    oracleText = "Flying, vigilance\n" +
        "Whenever Bruna attacks or blocks, $BRUNA_TRIGGER_TEXT"

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = brunaAttachAuras
        description = "Whenever Bruna attacks, $BRUNA_TRIGGER_TEXT"
    }

    triggeredAbility {
        trigger = Triggers.self.blocks()
        effect = brunaAttachAuras
        description = "Whenever Bruna blocks, $BRUNA_TRIGGER_TEXT"
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "208"
        artist = "Winona Nelson"
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e390bc78-31ad-4131-a9e4-93ee0c7c2f34.jpg?1783940656"
        ruling(
            "2012-05-01",
            "Only Auras that could legally enchant Bruna may be attached to it, whether they're being " +
                "moved from other permanents or being put onto the battlefield. An Aura with \"enchant " +
                "land,\" for example, could not be attached to Bruna this way."
        )
        ruling(
            "2012-05-01",
            "All of these Auras will become attached to Bruna before combat damage is dealt. If Bruna " +
                "is attacking, these Auras become attached before blockers are declared."
        )
    }
}
