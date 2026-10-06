package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Paralyze
 * {B}
 * Enchantment — Aura
 * Enchant creature
 * When this Aura enters, tap enchanted creature.
 * Enchanted creature doesn't untap during its controller's untap step.
 * At the beginning of the upkeep of enchanted creature's controller, that player may pay {4}. If
 * the player does, untap the creature.
 *
 * Tangle Kelp's ETB tap and untap lock, plus Erosion's ATTACHED-bound upkeep trigger: the trigger is
 * timed off the enchanted creature's controller, who is bound as the triggering player, so both the
 * "may" decision and the {4} fall on that player rather than on the Aura's controller. Per the 2007
 * ruling the Aura itself owns the trigger (it isn't granted to the creature).
 */
val Paralyze = card("Paralyze") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, tap enchanted creature.\n" +
        "Enchanted creature doesn't untap during its controller's untap step.\n" +
        "At the beginning of the upkeep of enchanted creature's controller, that player may pay {4}. " +
        "If the player does, untap the creature."
    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Tap(EffectTarget.EnchantedCreature)
        description = "When this Aura enters, tap enchanted creature."
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name, GroupFilter.attachedCreature())
    }

    triggeredAbility {
        trigger = Triggers.attached.beginningOf(Step.UPKEEP)
        effect = Effects.MayPay(
            cost = Effects.PayDynamicMana(
                amount = DynamicAmounts.fixed(4),
                payer = Player.TriggeringPlayer
            ),
            then = Effects.Untap(EffectTarget.EnchantedCreature),
            decisionMaker = EffectTarget.PlayerRef(Player.TriggeringPlayer),
            descriptionOverride = "Pay {4} to untap the creature enchanted by Paralyze?"
        )
        description = "At the beginning of the upkeep of enchanted creature's controller, that " +
            "player may pay {4}. If the player does, untap the creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "119"
        artist = "Anson Maddocks"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be33a155-de26-43d1-88f1-c926f1b7cb7c.jpg?1783948693"
        ruling("2007-09-16", "Paralyze itself has the last triggered ability, rather than granting that ability to the enchanted creature.")
        ruling("2004-10-04", "The creature can only be untapped using Paralyze once per turn.")
        ruling("2004-10-04", "Two Paralyzes are not cumulative. Paying either one will untap the creature.")
    }
}
