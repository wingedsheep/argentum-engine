package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kusari-Gama — Champions of Kamigawa #260
 * {3} · Artifact — Equipment
 *
 * Equipped creature has "{2}: This creature gets +1/+0 until end of turn."
 * Whenever equipped creature deals damage to a blocking creature, this Equipment deals that much
 * damage to each other creature defending player controls.
 * Equip {3}
 *
 * The damaged blocker is read from the damage event's last-known information, so a blocker the
 * damage killed is still "a blocking creature" (CR 603.10). A damage trigger binds the recipient
 * as its triggering entity, which names both halves of the effect: the blocker's controller is the
 * defending player, and `otherThanTriggeringEntity()` leaves the blocker itself out.
 */
val KusariGama = card("Kusari-Gama") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature has \"{2}: This creature gets +1/+0 until end of turn.\"\n" +
        "Whenever equipped creature deals damage to a blocking creature, this Equipment deals that " +
        "much damage to each other creature defending player controls.\n" +
        "Equip {3}"

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Mana("{2}")
                effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
            }
        )
    }

    triggeredAbility {
        trigger = Triggers.attached.dealsDamage(Recipient.Object(GameObjectFilter.Creature.blocking()))
        effect = Patterns.Group.dealDamageToAll(
            DynamicAmounts.triggerDamageAmount(),
            GroupFilter(
                GameObjectFilter.Creature.targetPlayerControls(EffectTarget.ControllerOfTriggeringEntity)
            ).otherThanTriggeringEntity()
        )
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "260"
        artist = "Tomas Giorello"
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c6a700bd-6424-4a0c-b055-e8b64cf430ec.jpg?1783944277"
    }
}
