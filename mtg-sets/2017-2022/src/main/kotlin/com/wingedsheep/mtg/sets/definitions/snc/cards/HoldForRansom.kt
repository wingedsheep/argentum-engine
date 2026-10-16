package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Hold for Ransom
 * {1}{W}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature can't attack or block and has "{7}: Hold for Ransom's controller sacrifices
 * it and draws a card. Activate only as a sorcery."
 *
 * The quoted ability belongs to the enchanted creature, so its controller — usually Hold for
 * Ransom's opponent — is the one who activates it (the card's ruling). "It" is Hold for Ransom:
 * [EffectTarget.GrantingSource] sacrificed by its own controller, and the draw goes to
 * [Player.ControllerOfGrantingSource], read off last-known information once the Aura is gone.
 */
val HoldForRansom = card("Hold for Ransom") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature can't attack or block and has \"{7}: Hold for Ransom's controller " +
        "sacrifices it and draws a card. Activate only as a sorcery.\""

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = CantAttack(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = CantBlock(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Mana("{7}")
                effect = Effects.SacrificeTarget(EffectTarget.GrantingSource, sacrificedByItsController = true) then
                    Effects.DrawCards(1, EffectTarget.PlayerRef(Player.ControllerOfGrantingSource))
                timing = TimingRule.SorcerySpeed
                description = "{7}: Hold for Ransom's controller sacrifices it and draws a card. " +
                    "Activate only as a sorcery."
            },
            filter = Filters.EnchantedCreature
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "16"
        artist = "Néstor Ossandón Leal"
        imageUri = "https://cards.scryfall.io/normal/front/5/3/53e74a99-11d9-40b8-b1d6-8f68a3f490fa.jpg?1783923159"
    }
}
