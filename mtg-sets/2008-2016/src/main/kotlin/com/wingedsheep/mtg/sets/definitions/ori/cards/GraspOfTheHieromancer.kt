package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Grasp of the Hieromancer (Magic Origins #15 — the card's earliest printing)
 * {1}{W} · Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets +1/+1 and has "Whenever this creature attacks, tap target creature
 * defending player controls."
 *
 * The quoted ability is a [GrantTriggeredAbility] (Clever Distraction's shape): "this creature" is
 * `Triggers.self.attacks()` bound to whatever creature has the ability, and the enchanted creature —
 * not the Aura — is its source, per the 2015-06-22 ruling. "Defending player" is the player that
 * creature attacks (CR 508.5), so the target filter is the referenced-player
 * `PlayerRef(Player.DefendingPlayer)`, resolved off the attacker while targets are chosen, rather
 * than `defendingPlayerControls()`, which would match every defending player in a multiplayer
 * combat.
 */
val GraspOfTheHieromancer = card("Grasp of the Hieromancer") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +1/+1 and has \"Whenever this creature attacks, tap target creature " +
        "defending player controls.\""

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.self.attacks()
                val defendersCreature = target(
                    TargetFilter.Creature.targetPlayerControls(EffectTarget.PlayerRef(Player.DefendingPlayer))
                )
                effect = Effects.Tap(defendersCreature)
            }
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "15"
        artist = "Igor Kieryluk"
        flavorText = "\"There is power in abandon, but far more in control.\"\n—Warden Hixus, to Kytheon"
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6a8740d2-7fcf-4bdf-afea-0c28c025370c.jpg?1783938362"
        ruling(
            "2015-06-22",
            "The triggered ability granted to the enchanted creature will tap the creature before " +
                "blockers are declared."
        )
        ruling(
            "2015-06-22",
            "The enchanted creature is the source of the triggered ability it gains, not Grasp of " +
                "the Hieromancer. If the enchanted creature isn't white, that ability can target a " +
                "creature with protection from white, for example."
        )
    }
}
