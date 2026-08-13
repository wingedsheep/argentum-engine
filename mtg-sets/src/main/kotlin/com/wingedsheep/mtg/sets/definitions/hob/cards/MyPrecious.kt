package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * My Precious // Allure of Power
 * {3}
 * Legendary Artifact — Equipment
 * Equipped creature has hexproof and can't be blocked.
 * Equip—{2}, Pay 2 life.
 *
 * Allure of Power — {1}{B}, Instant — Adventure
 * As an additional cost to cast this spell, sacrifice a creature.
 * Draw two cards.
 */
val MyPrecious = card("My Precious") {
    manaCost = "{3}"
    colorIdentity = "B"
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "Equipped creature has hexproof and can't be blocked.\n" +
        "Equip—{2}, Pay 2 life."

    staticAbility {
        ability = GrantKeyword(Keyword.HEXPROOF, Filters.EquippedCreature)
    }
    staticAbility {
        ability = CantBeBlocked(Filters.EquippedCreature)
    }

    activatedAbility {
        isEquipAbility = true
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.PayLife(2))
        val creature = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.AttachEquipment(creature)
        timing = TimingRule.SorcerySpeed
        description = "Equip—{2}, Pay 2 life."
    }

    adventure("Allure of Power") {
        manaCost = "{1}{B}"
        typeLine = "Instant — Adventure"
        oracleText = "As an additional cost to cast this spell, sacrifice a creature.\n" +
            "Draw two cards."
        additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Creature))
        spell {
            effect = Effects.DrawCards(2)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "176"
        artist = "Valera Lutfullina"
        flavorText = "\"It won't see us, will it, my precious?\""
        imageUri = "https://cards.scryfall.io/normal/front/1/5/15ae4d50-be2f-412c-bb6b-b0a06b60474a.jpg?1783902783"
    }
}
