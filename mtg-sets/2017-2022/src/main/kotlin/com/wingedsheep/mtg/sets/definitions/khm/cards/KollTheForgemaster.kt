package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Koll, the Forgemaster
 * {R}{W}
 * Legendary Creature — Dwarf Warrior
 * 2/2
 * Whenever another nontoken creature you control dies, if it was enchanted or equipped, return it
 * to its owner's hand.
 * Creature tokens you control that are enchanted or equipped get +1/+1.
 *
 * The first ability is a dies trigger, not a replacement: "if it was enchanted or equipped" reads
 * the dying creature's last-known attachments, which [StatePredicate.IsEnchanted] /
 * [StatePredicate.IsEquipped] answer on a dies trigger (as on Cass, Hand of Vengeance). The card
 * is returned from the graveyard by [EffectTarget.TriggeringEntity] (as on Halvar's Sword of the
 * Realms). The static bonus is a flat +1/+1 regardless of how many attachments a token carries.
 */
private val enchantedOrEquipped =
    StatePredicate.Or(listOf(StatePredicate.IsEnchanted, StatePredicate.IsEquipped))

val KollTheForgemaster = card("Koll, the Forgemaster") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Dwarf Warrior"
    power = 2
    toughness = 2
    oracleText = "Whenever another nontoken creature you control dies, if it was enchanted or equipped, " +
        "return it to its owner's hand.\n" +
        "Creature tokens you control that are enchanted or equipped get +1/+1."

    triggeredAbility {
        trigger = Triggers.another(
            GameObjectFilter.Creature.nontoken().youControl().withStatePredicate(enchantedOrEquipped)
        ).dies()
        effect = Effects.ReturnToHand(EffectTarget.TriggeringEntity)
    }

    staticAbility {
        ability = ModifyStats(
            1,
            1,
            GroupFilter(GameObjectFilter.Creature.token().youControl().withStatePredicate(enchantedOrEquipped))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "220"
        artist = "Bram Sels"
        flavorText = "Using tyrite from a fallen Cosmos monster, Koll forged a weapon to awe Halvar himself."
        imageUri = "https://cards.scryfall.io/normal/front/2/2/22f68b53-0cf7-434d-9da5-1d18c0828a46.jpg?1783928193"
        ruling(
            "2021-02-05",
            "A creature token you control is enchanted or equipped if there's an Aura or an Equipment " +
                "attached to it. You don't have to control that Aura or Equipment."
        )
        ruling(
            "2021-02-05",
            "A creature token you control that's enchanted or equipped gets only +1/+1 from Koll's last " +
                "ability, no matter how many Auras and Equipment are attached to that creature."
        )
    }
}
