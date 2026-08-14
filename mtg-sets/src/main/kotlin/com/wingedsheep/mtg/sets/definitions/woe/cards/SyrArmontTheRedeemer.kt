package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Syr Armont, the Redeemer
 * {3}{G}{W}
 * Legendary Creature — Human Knight
 * 4/4
 *
 * When Syr Armont enters, create a Monster Role token attached to another target creature you
 * control. (If you control another Role on it, put that one into the graveyard. Enchanted creature
 * gets +1/+1 and has trample.)
 * Enchanted creatures you control get +1/+1.
 *
 * Two halves that feed each other, both on existing primitives:
 *
 * 1. The ETB is [RedtoothGenealogist]'s exactly, with the Monster Role instead of the Royal one —
 *    "another target creature you control" is [Targets.OtherCreatureYouControl], so Syr Armont
 *    can't crown himself, and with no other creature the trigger has no legal target and leaves the
 *    stack. The one-Role-per-creature state-based action lives behind [Effects.CreateRoleToken].
 *
 * 2. The anthem is [ATaleForTheAges]'s group static, at +1/+1 instead of +2/+2: "enchanted" is
 *    *has an Aura attached*, and Role tokens are Auras (CR 113.2c), so the Role Syr Armont hands out
 *    is itself worth another +1/+1 on top of the Role's own bonus. Unlike A Tale for the Ages this
 *    anthem *can* pump its own source — Syr Armont is a creature you control, so an Aura on him
 *    turns it on for himself too.
 */
val SyrArmontTheRedeemer = card("Syr Armont, the Redeemer") {
    manaCost = "{3}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Human Knight"
    power = 4
    toughness = 4
    oracleText = "When Syr Armont enters, create a Monster Role token attached to another target " +
        "creature you control. (If you control another Role on it, put that one into the " +
        "graveyard. Enchanted creature gets +1/+1 and has trample.)\n" +
        "Enchanted creatures you control get +1/+1."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target("another target creature you control", Targets.OtherCreatureYouControl)
        effect = Effects.CreateRoleToken("Monster Role", creature)
    }

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.youControl().enchanted()),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "214"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/8/5/85050609-baf0-430a-ab33-83a6ea6d4741.jpg?1783915070"

        ruling(
            "2023-09-01",
            "Roles are colorless enchantment tokens. Each one has the Aura and Role subtypes and " +
                "the enchant creature ability."
        )
        ruling(
            "2023-09-01",
            "If a permanent has more than one Role attached to it controlled by the same player, " +
                "each of those Roles except the one with the most recent timestamp is put into its " +
                "owner's graveyard. This is a state-based action."
        )
        ruling(
            "2023-09-01",
            "Some spells and abilities that create Role tokens require targets. If each target " +
                "chosen is an illegal target as that spell or ability tries to resolve, it won't " +
                "resolve. The Role token won't be created."
        )
    }
}
