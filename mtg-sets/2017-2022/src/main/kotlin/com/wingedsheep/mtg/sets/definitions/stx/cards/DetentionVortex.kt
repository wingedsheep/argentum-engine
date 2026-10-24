package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Detention Vortex — Strixhaven: School of Mages #13 (canonical printing)
 * {W} · Enchantment — Aura
 *
 * Enchant nonland permanent
 * Enchanted permanent can't attack or block, and its activated abilities can't be activated.
 * {3}: Destroy this Aura. Only your opponents may activate this ability and only as a sorcery.
 *
 * "Only your opponents" is AnyPlayerMay narrowed by a condition whose "you" is the activating
 * player (Clergy of the Holy Nimbus); "only as a sorcery" is checked against that activating
 * player's turn and main phase, not the Aura controller's.
 */
val DetentionVortex = card("Detention Vortex") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant nonland permanent\n" +
        "Enchanted permanent can't attack or block, and its activated abilities can't be activated.\n" +
        "{3}: Destroy this Aura. Only your opponents may activate this ability and only as a sorcery."

    auraTarget = TargetObject(filter = TargetFilter(GameObjectFilter.NonlandPermanent))

    staticAbility {
        ability = CantAttack(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = CantBlock(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = PreventActivatedAbilities(
            filter = GameObjectFilter.Permanent.attachedToBySource(),
        )
    }

    activatedAbility {
        cost = Costs.Mana("{3}")
        timing = TimingRule.SorcerySpeed
        restrictions = listOf(
            ActivationRestriction.All(
                ActivationRestriction.AnyPlayerMay,
                ActivationRestriction.OnlyIfCondition(
                    Conditions.SourceMatches(GameObjectFilter.Any.opponentControls())
                )
            )
        )
        effect = Effects.Destroy(EffectTarget.Self)
        description = "{3}: Destroy this Aura. Only your opponents may activate this ability and only as a sorcery."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "13"
        artist = "Campbell White"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/54ebe581-be17-415a-a160-28d8f4e95636.jpg?1783927393"
    }
}
