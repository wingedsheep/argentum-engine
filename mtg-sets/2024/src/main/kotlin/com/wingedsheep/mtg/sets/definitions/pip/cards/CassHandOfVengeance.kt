package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cass, Hand of Vengeance
 * {2}{R}{W}
 * Legendary Creature — Human Ranger
 * 4/3
 * Vigilance
 * Whenever Cass or another creature you control dies, if it was enchanted or equipped, return any
 * number of Aura cards that were attached to it from your graveyard to the battlefield attached to
 * target creature, then attach any number of Equipment that were attached to it to that creature.
 *
 * "If it was enchanted or equipped" reads the dying creature's last-known attachments (CR 608.2h),
 * which `IsEnchanted` / `IsEquipped` already answer on a dies trigger. The Auras are gathered by
 * [CardSource.AttachedTo] with `zone = GRAVEYARD` — the frozen attachments that followed the
 * creature into your graveyard — and returned with `attachTo`, so an Aura the target can't legally
 * carry stays in the graveyard (the 2024-03-08 ruling). The Equipment are the same frozen
 * attachments still on the battlefield, attached one by one to the target.
 */
private val enchantedOrEquippedCreatureYouControl = GameObjectFilter.Creature.youControl()
    .withStatePredicate(StatePredicate.Or(listOf(StatePredicate.IsEnchanted, StatePredicate.IsEquipped)))

val CassHandOfVengeance = card("Cass, Hand of Vengeance") {
    manaCost = "{2}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Human Ranger"
    power = 4
    toughness = 3
    oracleText = "Vigilance\n" +
        "Whenever Cass or another creature you control dies, if it was enchanted or equipped, return " +
        "any number of Aura cards that were attached to it from your graveyard to the battlefield " +
        "attached to target creature, then attach any number of Equipment that were attached to it " +
        "to that creature."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.a(enchantedOrEquippedCreatureYouControl).dies()
        val creature = target(TargetFilter.Creature)
        effect = Effects.Pipeline {
            val auraCards = gather(
                CardSource.AttachedTo(
                    host = EffectTarget.TriggeringEntity,
                    filter = GameObjectFilter.Any.withSubtype(Subtype.AURA),
                    zone = Zone.GRAVEYARD
                )
            )
            val chosenAuras = chooseAnyNumber(
                auraCards,
                prompt = "Choose any number of Aura cards to return attached to the target creature"
            )
            move(chosenAuras, CardDestination.ToZone(Zone.BATTLEFIELD), attachTo = creature)
            val equipment = gather(
                CardSource.AttachedTo(
                    host = EffectTarget.TriggeringEntity,
                    filter = GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT)
                )
            )
            val chosenEquipment = chooseAnyNumber(
                equipment,
                prompt = "Choose any number of Equipment to attach to the target creature",
                useTargetingUI = true
            )
            run(
                Effects.ForEachInCollection(
                    chosenEquipment,
                    Effects.AttachTargetEquipmentToCreature(
                        equipmentTarget = EffectTarget.IterationEntity,
                        creatureTarget = creature
                    )
                )
            )
        }
        description = "Whenever Cass or another creature you control dies, if it was enchanted or " +
            "equipped, return any number of Aura cards that were attached to it from your graveyard to " +
            "the battlefield attached to target creature, then attach any number of Equipment that were " +
            "attached to it to that creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "97"
        artist = "Dan Watson"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b6d7fa6-4de6-4bc8-b250-2ed6eb559618.jpg?1783912391"
        ruling(
            "2024-03-08",
            "You can't return Aura cards that can't legally be attached to the target creature. For " +
                "example, if the target creature has protection from red, you can't return Grim Reaper's " +
                "Sprint to the battlefield with Cass's last ability."
        )
    }
}
