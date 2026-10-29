package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Archfiend's Vessel
 * {B}
 * Creature — Human Cleric
 * 1/1
 * Lifelink
 * When this creature enters, if it entered from your graveyard or you cast it from your graveyard,
 * exile it. If you do, create a 5/5 black Demon creature token with flying.
 *
 * The intervening-if (CR 603.4) is [Conditions.TriggeringEntityEnteredOrWasCastFromGraveyard],
 * which only knows "a graveyard". A card is only ever in its owner's graveyard, so "your graveyard"
 * is that plus "you own it" — a Vessel you reanimated out of an opponent's graveyard, or cast from
 * theirs, does nothing (the Grist, Voracious Larva shape).
 *
 * The exile is scoped `fromZone = BATTLEFIELD` and gated by [Effects.IfYouDo]: if the Vessel has
 * left the battlefield while the trigger was on the stack it can't be exiled from its new zone, so
 * no Demon is created (2020-06-23 ruling) — the Mazemind Tome shape.
 */
val ArchfiendsVessel = card("Archfiend's Vessel") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 1
    oracleText = "Lifelink (Damage dealt by this creature also causes you to gain that much life.)\n" +
        "When this creature enters, if it entered from your graveyard or you cast it from your " +
        "graveyard, exile it. If you do, create a 5/5 black Demon creature token with flying."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.All(
            Conditions.TriggeringEntityEnteredOrWasCastFromGraveyard,
            Conditions.SourceMatches(GameObjectFilter.Any.ownedByYou()),
        )
        effect = Effects.IfYouDo(
            action = Effects.Move(
                target = EffectTarget.Self,
                destination = Zone.EXILE,
                fromZone = Zone.BATTLEFIELD,
            ),
            then = Effects.CreateToken(
                power = 5,
                toughness = 5,
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Demon"),
                keywords = setOf(Keyword.FLYING),
                imageUri = "https://cards.scryfall.io/normal/front/f/5/f54c7f9f-a1ea-4bf0-8ab3-cca49f393f4a.jpg?1783930591",
            ),
        )
        description = "When this creature enters, if it entered from your graveyard or you cast it " +
            "from your graveyard, exile it. If you do, create a 5/5 black Demon creature token with flying."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "88"
        artist = "Jake Murray"
        imageUri = "https://cards.scryfall.io/normal/front/8/3/83437022-ba00-4370-83c2-ce1260336fcc.jpg?1783930712"
        ruling(
            "2020-06-23",
            "If Archfiend's Vessel leaves the battlefield while its triggered ability is on the stack, " +
                "you can't exile it from the zone it's put into, so you won't create a Demon."
        )
    }
}
