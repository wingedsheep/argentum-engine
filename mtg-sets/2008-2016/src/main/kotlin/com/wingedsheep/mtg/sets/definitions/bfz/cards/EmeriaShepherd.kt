package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Emeria Shepherd — Battle for Zendikar #22
 * {5}{W}{W} · Creature — Angel · 4/4
 * Flying
 * Landfall — Whenever a land you control enters, you may return target nonland permanent card from
 * your graveyard to your hand. If that land is a Plains, you may return that nonland permanent card
 * to the battlefield instead.
 *
 * The target is chosen as the trigger goes on the stack; the "may" choices happen on resolution.
 * "If that land is a Plains" reads the triggering land at resolution (Koth, the Geomancer's
 * `EntityMatches(TriggeringEntity, …)`, matched against projected state while it is on the
 * battlefield). With a Plains the controller is first offered the battlefield; declining that falls back
 * to the ordinary "you may return it to your hand", so all three outcomes (battlefield, hand,
 * nothing) stay reachable. Without a Plains only the hand return is offered.
 */
val EmeriaShepherd = card("Emeria Shepherd") {
    manaCost = "{5}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "Landfall — Whenever a land you control enters, you may return target nonland permanent card " +
        "from your graveyard to your hand. If that land is a Plains, you may return that nonland " +
        "permanent card to the battlefield instead."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        val returned = target(TargetFilter(GameObjectFilter.NonlandPermanent.ownedByYou(), zone = Zone.GRAVEYARD))
        val toHand = Effects.May(
            Effects.Move(returned, Zone.HAND, fromZone = Zone.GRAVEYARD),
            prompt = "Return the targeted card to your hand?",
        )
        effect = Effects.If(
            condition = Conditions.EntityMatches(
                EffectTarget.TriggeringEntity,
                GameObjectFilter.Land.withSubtype("Plains"),
            ),
            then = Effects.May(
                Effects.Move(returned, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD),
                otherwise = toHand,
                prompt = "Return the targeted card to the battlefield instead of your hand?",
            ),
            otherwise = toHand,
        )
        description = "Landfall — Whenever a land you control enters, you may return target nonland " +
            "permanent card from your graveyard to your hand. If that land is a Plains, you may return " +
            "that nonland permanent card to the battlefield instead."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "22"
        artist = "Cynthia Sheppard"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c366f25-93c7-4088-88a4-64d3ee18a89a.jpg?1783938221"
        ruling("2015-08-25", "If you return an Aura to the battlefield this way, you choose what it will enchant as it enters the battlefield. This doesn't target any player or permanent, so you could attach an Aura to a permanent with hexproof controlled by an opponent, for example. If there's nothing the Aura can legally enchant, it stays in the graveyard.")
    }
}
