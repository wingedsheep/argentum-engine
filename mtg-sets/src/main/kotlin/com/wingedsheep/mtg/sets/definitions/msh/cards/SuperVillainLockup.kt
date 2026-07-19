package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Super Villain Lockup — Marvel Super Heroes #37
 * {1}{W} · Enchantment
 *
 * Flash
 * When this enchantment enters, exile target tapped creature an opponent controls until this
 * enchantment leaves the battlefield.
 *
 * Oblivion Ring shape: the ETB trigger exiles with [Effects.ExileUntilLeaves] (which records the
 * link on the source), and the leaves-the-battlefield trigger returns the linked exile with
 * [Effects.ReturnLinkedExileUnderOwnersControl]. The exile is *not* optional here (unlike
 * Suspension Field) — the trigger is mandatory, so it fizzles only when the target is gone.
 */
val SuperVillainLockup = card("Super Villain Lockup") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Flash\n" +
        "When this enchantment enters, exile target tapped creature an opponent controls until " +
        "this enchantment leaves the battlefield."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target(
            "tapped creature an opponent controls",
            TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.tapped().opponentControls())),
        )
        effect = Effects.ExileUntilLeaves(creature)
        description = "When this enchantment enters, exile target tapped creature an opponent " +
            "controls until this enchantment leaves the battlefield."
    }

    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "37"
        artist = "Jurijus Chitrovas"
        flavorText = "\"Welcome to the Raft, pal.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1af5a1ca-3d11-45e3-ba12-c455c5a7fea1.jpg?1783902965"
    }
}
