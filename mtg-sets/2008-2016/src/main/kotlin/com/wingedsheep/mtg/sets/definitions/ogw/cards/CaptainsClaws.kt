package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender

/**
 * Captain's Claws — Oath of the Gatewatch #162
 * {2} · Artifact — Equipment
 *
 * Equipped creature gets +1/+0.
 * Whenever equipped creature attacks, create a 1/1 white Kor Ally creature token that's tapped
 * and attacking.
 * Equip {1}
 *
 * The token enters tapped and attacking a defender its controller chooses as it enters (CR 508.4)
 * — not necessarily the one the equipped creature attacks — so [EnteringAttackDefender.CONTROLLER_CHOOSES].
 * It was never declared as an attacker, so "whenever a creature attacks" triggers don't see it.
 */
val CaptainsClaws = card("Captain's Claws") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+0.\n" +
        "Whenever equipped creature attacks, create a 1/1 white Kor Ally creature token that's tapped and attacking.\n" +
        "Equip {1}"

    staticAbility {
        ability = ModifyStats(1, 0, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.attached.attacks()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Kor", "Ally"),
            tapped = true,
            attacking = true,
            attackDefender = EnteringAttackDefender.CONTROLLER_CHOOSES,
            imageUri = "https://cards.scryfall.io/normal/front/b/e/be224180-a482-4b94-8a9d-3a92ee0eb34b.jpg?1783938120",
        )
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "162"
        artist = "Anthony Palumbo"
        flavorText = "Forged for victory in a time of despair."
        imageUri = "https://cards.scryfall.io/normal/front/5/2/521ee576-5a51-4353-af38-c461f05d66ce.jpg?1783937895"

        ruling("2016-01-22", "You choose which opponent or planeswalker controlled by an opponent the token is attacking as it enters the battlefield. It doesn't have to be the same opponent or planeswalker that the equipped creature is attacking.")
        ruling("2016-01-22", "The token is never declared as an attacking creature. It simply enters the battlefield attacking. This won't cause any abilities that trigger \"whenever a creature attacks\" to trigger.")
    }
}
