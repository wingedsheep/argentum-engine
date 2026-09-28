package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Furnace Reins
 * {2}{R}
 * Sorcery
 * Gain control of target creature until end of turn. Untap that creature. Until end of turn, it
 * gains haste and "Whenever this creature deals combat damage to a player or battle, create a
 * Treasure token."
 *
 * Threaten shape (Flash Conscription) with a granted combat-damage trigger. The Treasure goes to
 * the ability's controller, i.e. whoever controls the creature when it connects.
 */
val FurnaceReins = card("Furnace Reins") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Gain control of target creature until end of turn. Untap that creature. Until end " +
        "of turn, it gains haste and \"Whenever this creature deals combat damage to a player or " +
        "battle, create a Treasure token.\" (It's an artifact with \"{T}, Sacrifice this token: " +
        "Add one mana of any color.\")"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.GainControl(creature, Duration.EndOfTurn) then
            Effects.Untap(creature) then
            Effects.GrantKeyword(Keyword.HASTE, creature) then
            Effects.GrantTriggeredAbility(
                ability = TriggeredAbility.create(
                    trigger = Triggers.self.dealsCombatDamage(
                        Recipient.AnyOf(listOf(Recipient.AnyPlayer, Recipient.Object(GameObjectFilter.Battle)))
                    ),
                    effect = Effects.CreateTreasure(),
                    descriptionOverride = "Whenever this creature deals combat damage to a player or battle, create a Treasure token."
                ),
                target = creature,
                duration = Duration.EndOfTurn
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "141"
        artist = "Brian Valeza"
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e91bfea6-0e80-4239-bede-7cb971e64c1a.jpg?1783916991"
    }
}
