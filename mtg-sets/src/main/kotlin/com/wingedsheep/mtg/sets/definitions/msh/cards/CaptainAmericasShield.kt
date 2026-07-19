package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggerBinding

/**
 * Captain America's Shield (MSH #244) — {2} Legendary Artifact — Equipment
 *
 * Indestructible
 * Equipped creature gets +0/+8 and has vigilance.
 * Whenever equipped creature attacks, tap target creature defending player controls.
 * Equip {2}
 *
 * Implementation notes:
 * - Indestructible is printed on the Equipment itself (not granted to the equipped creature),
 *   so it's wired via [keywords] — same shape as Diamond Pick-Axe.
 * - The +0/+8 pump and the vigilance grant are two static abilities scoped to
 *   [Filters.EquippedCreature].
 * - The attack trigger lives on the Equipment and binds to the attached creature
 *   ([TriggerBinding.ATTACHED]), matching Thunder Lasso's identical
 *   "Whenever equipped creature attacks, tap target creature defending player controls."
 */
val CaptainAmericasShield = card("Captain America's Shield") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "Indestructible\n" +
        "Equipped creature gets +0/+8 and has vigilance.\n" +
        "Whenever equipped creature attacks, tap target creature defending player controls.\n" +
        "Equip {2}"

    // The Equipment itself is indestructible.
    keywords(Keyword.INDESTRUCTIBLE)

    // Equipped creature gets +0/+8.
    staticAbility {
        ability = ModifyStats(0, +8, Filters.EquippedCreature)
    }

    // ...and has vigilance.
    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, Filters.EquippedCreature)
    }

    // Whenever equipped creature attacks, tap target creature defending player controls.
    triggeredAbility {
        trigger = Triggers.attacks(binding = TriggerBinding.ATTACHED)
        val creature = target("creature defending player controls", Targets.CreatureOpponentControls)
        effect = Effects.Tap(creature)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "244"
        artist = "Eglė Mosakaitė"
        flavorText = "It defends what is and stands for what could be."
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b5433ac-0d36-4472-bf8b-d22f0ffd367b.jpg?1783902892"
    }
}
