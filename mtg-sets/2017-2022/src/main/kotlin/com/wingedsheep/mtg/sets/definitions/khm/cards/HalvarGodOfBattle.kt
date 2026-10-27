package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Halvar, God of Battle // Sword of the Realms — Kaldheim #15 (canonical printing)
 * Legendary Creature — God 4/4 // Legendary Artifact — Equipment · Modal double-faced card
 *
 * Front: "enchanted or equipped" is the Aura/Equipment-agnostic [StatePredicate.IsEnchanted] /
 * [StatePredicate.IsEquipped] pair — the ruling says you needn't control the attachment. The combat
 * trigger fires at the beginning of *each* combat (any player's turn); its first target is an Aura or
 * Equipment of any controller attached to a creature you control (Miracle Worker's
 * [StatePredicate.AttachedTo]), its second a creature you control. The attach goes through the shared
 * attachment mover, so an Aura that couldn't legally enchant the new creature, or an attachment
 * already on it, doesn't move (rulings / CR 701.3b).
 *
 * Back: "return it to its owner's hand" is Demonic Vigor's `Triggers.attached.dies()` return of the
 * triggering card — a card that left the graveyard first, or a token, stays where it is (rulings).
 */
private val enchantedOrEquippedCreatureYouControl = GameObjectFilter.Creature.youControl()
    .withStatePredicate(StatePredicate.Or(listOf(StatePredicate.IsEnchanted, StatePredicate.IsEquipped)))

private val auraOrEquipmentAttachedToCreatureYouControl =
    (GameObjectFilter.Any.withSubtype(Subtype.AURA) or GameObjectFilter.Any.withSubtype(Subtype.EQUIPMENT))
        .withStatePredicate(StatePredicate.AttachedTo(GameObjectFilter.Creature.youControl()))

private val HalvarGodOfBattleFront = card("Halvar, God of Battle") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — God"
    power = 4
    toughness = 4
    oracleText = "Creatures you control that are enchanted or equipped have double strike.\n" +
        "At the beginning of each combat, you may attach target Aura or Equipment attached to a " +
        "creature you control to target creature you control."

    staticAbility {
        ability = GrantKeyword(Keyword.DOUBLE_STRIKE, GroupFilter(enchantedOrEquippedCreatureYouControl))
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.BEGIN_COMBAT)
        val attachment = target(TargetFilter(auraOrEquipmentAttachedToCreatureYouControl))
        val creature = target(TargetFilter(GameObjectFilter.Creature.youControl()))
        effect = Effects.May(Effects.AttachTargetEquipmentToCreature(attachment, creature))
        description = "At the beginning of each combat, you may attach target Aura or Equipment " +
            "attached to a creature you control to target creature you control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "15"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/97502411-5c93-434c-b77b-ceb2c32feae7.jpg?1783928291"
        ruling("2021-02-05", "A creature you control is enchanted or equipped if there's an Aura or an Equipment attached to it. You don't have to control that Aura or Equipment.")
        ruling("2021-02-05", "Similarly, you don't have to control the Aura or Equipment that you choose as a target of Halvar's triggered ability, but it must be attached to a creature you control.")
        ruling("2021-02-05", "If either target of the triggered ability is an illegal target as the ability resolves, the ability won't do anything. If both targets are illegal, the ability won't resolve. If the Aura or Equipment is already attached to the target creature, nothing happens.")
    }
}

private val SwordOfTheRealmsBack = card("Sword of the Realms") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+0 and has vigilance.\n" +
        "Whenever equipped creature dies, return it to its owner's hand.\n" +
        "Equip {1}{W}"

    staticAbility {
        ability = ModifyStats(2, 0)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE)
    }

    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Effects.ReturnToHand(EffectTarget.TriggeringEntity)
        description = "Whenever equipped creature dies, return it to its owner's hand."
    }

    equipAbility("{1}{W}")

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "15"
        artist = "Lie Setiawan"
        flavorText = "It cuts through the Cosmos itself, carving new Omenpaths between the realms."
        imageUri = "https://cards.scryfall.io/normal/back/9/7/97502411-5c93-434c-b77b-ceb2c32feae7.jpg?1783928291"
        ruling("2021-02-05", "If the creature equipped by Sword of the Realms dies but leaves the graveyard before the triggered ability resolves, it won't be returned to its owner's hand. It will just stay wherever it went.")
        ruling("2021-02-05", "If Sword of the Realms is attached to a token creature that dies, the Sword's triggered ability will trigger, but the token will cease to exist after going to the graveyard. It won't be returned to its owner's hand.")
    }
}

val HalvarGodOfBattle: CardDefinition = CardDefinition.modalDoubleFacedPermanent(
    frontFace = HalvarGodOfBattleFront,
    backFace = SwordOfTheRealmsBack,
)
