package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Cult of the Waxing Moon
 * {4}{G}
 * Creature — Human Shaman
 * 5/4
 * Whenever a permanent you control transforms into a non-Human creature, create a 2/2 green Wolf
 * creature token.
 *
 * "Transforms into a non-Human creature" is read off the face that's up after the transform, in
 * either direction, whatever the permanent was before (ruling 2016-04-08) — the transform trigger's
 * subject filter is matched against post-transform projected state. A permanent that enters the
 * battlefield transformed never transforms, so it emits no transform event and doesn't trigger.
 */
val CultOfTheWaxingMoon = card("Cult of the Waxing Moon") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Shaman"
    power = 5
    toughness = 4
    oracleText = "Whenever a permanent you control transforms into a non-Human creature, create a 2/2 " +
        "green Wolf creature token."

    triggeredAbility {
        trigger = Triggers.a(
            GameObjectFilter.Creature.youControl().notSubtype(Subtype("Human"))
        ).transforms()
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Wolf")
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "201"
        artist = "Anastasia Ovchinnikova"
        flavorText = "In times of madness, people cling to the familiar, monstrous though it may be."
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d2a13669-4656-476a-8984-ec3d47960907.jpg?1783937733"
        ruling("2016-04-08", "A permanent transforms into a non-Human creature if it transforms and is a creature without the creature type Human after transforming, regardless of whether it was a Human creature (or a creature at all) before it transformed. For example, Westvale Abbey and Thraben Gargoyle will both cause Cult of the Waxing Moon's ability to trigger when they transform.")
        ruling("2016-04-08", "A non-Human creature that's put onto the battlefield transformed, such as Skin Shedder, will not cause Cult of the Waxing Moon's ability to trigger.")
    }
}
