package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Leather Armor
 * {1}
 * Artifact — Equipment
 * Equipped creature gets +0/+1 and has ward {1}.
 * Equip {0}. Activate only once each turn.
 *
 * `equipAbility` takes no activation restriction, so the equip is an equip-flagged activated
 * ability (sorcery speed, eligible for equip-keyed rules) restricted to once per turn — the
 * Shredder's Armor / Dark Knight's Greatsword shape with a {0} mana cost.
 */
val LeatherArmor = card("Leather Armor") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +0/+1 and has ward {1}. (Whenever equipped creature becomes the target of a spell or ability an opponent controls, counter it unless that player pays {1}.)\n" +
        "Equip {0}. Activate only once each turn."

    staticAbility {
        ability = ModifyStats(0, 1, Filters.EquippedCreature)
    }
    staticAbility {
        ability = GrantWard(WardCost.Mana("{1}"), Filters.EquippedCreature)
    }

    activatedAbility {
        isEquipAbility = true
        cost = Costs.Mana("{0}")
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AttachEquipment(creature)
        timing = TimingRule.SorcerySpeed
        restrictions = listOf(ActivationRestriction.OncePerTurn)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "248"
        artist = "Martina Pilcerova"
        flavorText = "Lightweight, flexible, and protective."
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e62d4e76-cdb0-48c7-aa1c-b1f514057b68.jpg?1783926437"
    }
}
