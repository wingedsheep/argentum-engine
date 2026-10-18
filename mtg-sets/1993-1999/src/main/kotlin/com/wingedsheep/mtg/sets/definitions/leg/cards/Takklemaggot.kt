package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Takklemaggot — {2}{B}{B} Enchantment — Aura.
 *
 * When the enchanted creature dies, that creature's controller (last-known) picks the next host
 * among the creatures this card could enchant — any player's, not targeted. With none to pick, it
 * comes back as a non-Aura enchantment that pings that player each upkeep. Either way it returns
 * only if it is still in the graveyard when the trigger resolves.
 */
val Takklemaggot = card("Takklemaggot") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "At the beginning of the upkeep of enchanted creature's controller, put a -0/-1 counter on that creature.\n" +
        "When enchanted creature dies, that creature's controller chooses a creature that this card could enchant. " +
        "If the player does, return this card to the battlefield under your control attached to that creature. " +
        "If they don't, return this card to the battlefield under your control as a non-Aura enchantment. " +
        "It loses \"enchant creature\" and gains \"At the beginning of that player's upkeep, this enchantment " +
        "deals 1 damage to that player.\""
    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.attached.beginningOf(Step.UPKEEP)
        effect = Effects.AddCounters(CounterType.MINUS_ZERO_MINUS_ONE, 1, EffectTarget.EnchantedCreature)
    }

    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Effects.If(
            Conditions.SourceInZone(Zone.GRAVEYARD),
            Effects.PutOntoBattlefieldAttachedToChosen(
                target = EffectTarget.Self,
                hostFilter = GameObjectFilter.Creature,
                chooser = Chooser.ControllerOfTriggeringEntity,
            )
        ) then Effects.If(
            Conditions.SourceInZone(Zone.GRAVEYARD),
            Effects.PutOntoBattlefieldAsNonAura(
                grantedTriggeredAbilities = listOf(
                    TriggeredAbility.create(
                        trigger = Triggers.player(Player.ChosenPlayer).beginningOf(Step.UPKEEP),
                        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.TriggeringPlayer)),
                    )
                ),
                chosenPlayer = EffectTarget.ControllerOfTriggeringEntity,
            )
        )
        description = "When enchanted creature dies, that creature's controller chooses a creature that this " +
            "card could enchant. If the player does, return this card to the battlefield under your control " +
            "attached to that creature. If they don't, return this card to the battlefield under your control " +
            "as a non-Aura enchantment. It loses \"enchant creature\" and gains \"At the beginning of that " +
            "player's upkeep, this enchantment deals 1 damage to that player.\""
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "119"
        artist = "Daniel Gelon"
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d8f49b3d-7fcb-4169-9298-cdf7a1dbe3f5.jpg?1783948062"
    }
}
