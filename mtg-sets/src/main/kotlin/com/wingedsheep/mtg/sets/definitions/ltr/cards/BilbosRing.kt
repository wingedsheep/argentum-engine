package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.AttachEquipmentEffect
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Bilbo's Ring
 * {3}
 * Legendary Artifact — Equipment
 * During your turn, equipped creature has hexproof and can't be blocked.
 * Whenever equipped creature attacks alone, you draw a card and you lose 1 life.
 * Equip Halfling {1}
 * Equip {4}
 */
val BilbosRing = card("Bilbo's Ring") {
    manaCost = "{3}"
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "During your turn, equipped creature has hexproof and can't be blocked.\n" +
        "Whenever equipped creature attacks alone, you draw a card and you lose 1 life.\n" +
        "Equip Halfling {1} ({1}: Attach to target Halfling you control. Equip only as a sorcery.)\n" +
        "Equip {4} ({4}: Attach to target creature you control. Equip only as a sorcery.)"

    // During your turn, equipped creature has hexproof...
    staticAbility {
        ability = GrantKeyword(Keyword.HEXPROOF, Filters.EquippedCreature)
        condition = Conditions.IsYourTurn
    }

    // ...and can't be blocked.
    staticAbility {
        ability = CantBeBlocked(Filters.EquippedCreature)
        condition = Conditions.IsYourTurn
    }

    // Whenever equipped creature attacks alone, you draw a card and you lose 1 life.
    triggeredAbility {
        trigger = Triggers.attacks(
            binding = TriggerBinding.ATTACHED,
            requires = setOf(AttackPredicate.Alone)
        )
        effect = Effects.DrawCards(1)
            .then(Effects.LoseLife(1, EffectTarget.PlayerRef(Player.You)))
    }

    // Equip Halfling {1}: Attach to target Halfling you control. Equip only as a sorcery.
    activatedAbility {
        cost = Costs.Mana("{1}")
        timing = TimingRule.SorcerySpeed
        val halfling = target(
            "Halfling you control",
            TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.withSubtype("Halfling").youControl()))
        )
        effect = AttachEquipmentEffect(target = halfling)
    }

    // Equip {4}: Attach to target creature you control. Equip only as a sorcery.
    equipAbility("{4}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "298"
        artist = "Randy Gallegos"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/91dbeac4-1c39-4a4f-84e7-1b71f7468c8f.jpg?1687424902"
    }
}
