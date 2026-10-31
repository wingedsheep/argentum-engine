package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Boneclad Necromancer — Core Set 2020 #93
 * {3}{B}{B} · Creature — Human Wizard · Common
 * 3/3
 *
 * When this creature enters, you may exile target creature card from a graveyard. If you do,
 * create a 2/2 black Zombie creature token.
 *
 * The target is chosen as the trigger goes on the stack (no creature card in any graveyard ⇒ the
 * trigger is removed); the "may" is a resolution-time yes/no gating both the exile and the token.
 */
val BonecladNecromancer = card("Boneclad Necromancer") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Wizard"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, you may exile target creature card from a graveyard. " +
        "If you do, create a 2/2 black Zombie creature token."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creatureCard = target(TargetFilter.CreatureInGraveyard)
        effect = Effects.May(
            Effects.Exile(creatureCard, fromZone = Zone.GRAVEYARD) then
                Effects.CreateToken(
                    power = 2,
                    toughness = 2,
                    colors = setOf(Color.BLACK),
                    creatureTypes = setOf("Zombie"),
                    imageUri = "https://cards.scryfall.io/normal/front/1/8/18f0436e-9328-4266-9cf8-80b557a0c17c.jpg?1783932856"
                )
        )
        description = "When this creature enters, you may exile target creature card from a " +
            "graveyard. If you do, create a 2/2 black Zombie creature token."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "93"
        artist = "Robbie Trevino"
        flavorText = "\"Living or dead, everyone responds to the proper motivation.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab9ed2b9-5fb9-4633-8c9a-0594ae7c49c7.jpg?1783932996"
    }
}
