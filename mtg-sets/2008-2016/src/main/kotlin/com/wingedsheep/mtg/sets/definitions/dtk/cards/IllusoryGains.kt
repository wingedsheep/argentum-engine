package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ControlEnchantedPermanent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Illusory Gains
 * {3}{U}{U}
 * Enchantment — Aura
 * Enchant creature
 * You control enchanted creature.
 * Whenever a creature an opponent controls enters, attach this Aura to that creature.
 *
 * Control is Control Magic's [ControlEnchantedPermanent], which applies to whatever the Aura is
 * attached to — so moving the Aura hands the old creature back and takes the new one in one step.
 *
 * The move is mandatory and doesn't target (hexproof and shroud don't stop it), but it only happens
 * if the attach is legal (CR 701.3b): "that creature" must still be on the battlefield (the gather
 * over [StatePredicate.IsTriggeringEntity]), and Illusory Gains must still be on the battlefield,
 * not be a creature itself, and be able to enchant it — its enchant restriction plus protection
 * from blue, read by `couldAttachTo`. Otherwise it stays where it is and you keep the creature it
 * already enchants. `AttachEquipment` is attachment-generic (Stasis Cell, Kudzu): it detaches from
 * the old host first and renews the Aura's timestamp.
 */
val IllusoryGains = card("Illusory Gains") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "You control enchanted creature.\n" +
        "Whenever a creature an opponent controls enters, attach this Aura to that creature."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ControlEnchantedPermanent
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.opponentControls()).enters()
        effect = Effects.Pipeline {
            val thatCreature = gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Any.withStatePredicate(StatePredicate.IsTriggeringEntity)
                )
            )
            ifNotEmpty(thatCreature) {
                run(
                    Effects.If(
                        Conditions.EntityMatches(
                            EffectTarget.Self,
                            GameObjectFilter.Any
                                .onBattlefield()
                                .notCreature()
                                .couldAttachTo(EffectTarget.TriggeringEntity)
                        ),
                        Effects.AttachEquipment(thatCreature.asTarget)
                    )
                )
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "59"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/747dfb21-a00c-46ba-9da3-739f570e1246.jpg?1783938607"
        ruling(
            "2015-02-25",
            "Illusory Gains's last ability is mandatory. You can't choose to have Illusory Gains not move."
        )
        ruling(
            "2015-02-25",
            "If Illusory Gains can't legally enchant the creature that enters the battlefield (perhaps " +
                "because it has protection from blue), it remains where it is. You retain control of the " +
                "creature it's currently enchanting."
        )
        ruling(
            "2015-02-25",
            "The last ability of Illusory Gains doesn't target the new creature. It will become attached " +
                "to that creature even if that creature has hexproof or shroud."
        )
        ruling(
            "2015-02-25",
            "If multiple creatures enter the battlefield under an opponent's control at the same time, " +
                "Illusory Gains will trigger for each of them. You may put these abilities on the stack in " +
                "any order. As each resolves, you'll briefly gain control of the associated creature. The " +
                "last one to resolve determines which one you'll ultimately control."
        )
    }
}
